package iuh.fit.socialservice.infrastructure.activity;

import iuh.fit.socialservice.application.port.external.ActivityEventPublisher;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Nạp bù một lần: lượt thích, lưu, bình luận, chia sẻ có từ trước khi có gợi ý cá nhân hóa được phát sang ai-service
 * (giữ đúng thời điểm gốc để độ cũ được tính đúng). Chỉ đánh dấu xong khi Kafka đã nhận hết; Kafka chưa lên thì lượt
 * sau thử lại. Phát lại vô hại: ai-service ghi đè theo mã cố định của từng hành vi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "activity.events.enabled", havingValue = "true")
public class ActivityBackfillJob {
    static final String MARKER_COLLECTION = "activity_backfills";
    static final String POSTS = "post-activity-v1";
    private static final int CHUNK = 500;

    private final MongoTemplate mongo;
    private final ActivityEventKafkaPublisher publisher;

    private record Pending(String eventType, UUID userId, String postId, Map<String, Object> extra, LocalDateTime at) { }

    @Scheduled(initialDelayString = "${activity.backfill.delay-ms:90000}",
            fixedDelayString = "${activity.backfill.retry-ms:600000}")
    public void backfill() {
        if (mongo.exists(Query.query(Criteria.where("_id").is(POSTS)), MARKER_COLLECTION)) return;
        int sent = 0;
        try {
            sent += drain(mongo.stream(Query.query(Criteria.where("type").in(ReactionType.LIKE, ReactionType.SAVE)),
                    PostReactionEntity.class).map(reaction -> new Pending(ActivityEventPublisher.POST_REACTION,
                    UUID.fromString(reaction.getUserId()), reaction.getPostId(),
                    Map.of("reaction", reaction.getType().name(), "active", true), reaction.getReactedAt())));
            sent += drain(mongo.stream(Query.query(Criteria.where("status").is(PostStatus.PUBLISHED)),
                    PostCommentEntity.class).map(comment -> new Pending(ActivityEventPublisher.POST_COMMENT,
                    UUID.fromString(comment.getAuthorId()), comment.getPostId(),
                    Map.of("commentId", comment.getCommentId(), "active", true), comment.getCreatedAt())));
            sent += drain(mongo.stream(Query.query(Criteria.where("sharedPostId").ne(null)
                            .and("status").is(PostStatus.PUBLISHED)), PostEntity.class)
                    .map(share -> new Pending(ActivityEventPublisher.POST_SHARED, UUID.fromString(share.getAuthorId()),
                            share.getSharedPostId(), Map.of(), share.getPublishedAt())));
        } catch (Exception exception) {
            log.info("Activity backfill paused after {} events, retrying later: {}", sent, exception.getMessage());
            return;
        }
        mongo.save(new Document("_id", POSTS).append("completedAt", Instant.now()).append("eventCount", sent),
                MARKER_COLLECTION);
        log.info("Activity backfill done: {} events", sent);
    }

    /** Gửi theo lô: tra tác giả và môn của các bài trong lô một lần, chờ Kafka nhận cả lô rồi mới đi tiếp. */
    private int drain(Stream<Pending> items) throws Exception {
        int sent = 0;
        List<Pending> chunk = new ArrayList<>(CHUNK);
        try (items) {
            var iterator = items.iterator();
            while (iterator.hasNext()) {
                chunk.add(iterator.next());
                if (chunk.size() == CHUNK || !iterator.hasNext()) {
                    sent += send(chunk);
                    chunk.clear();
                }
            }
        }
        return sent;
    }

    private int send(List<Pending> chunk) throws Exception {
        Map<String, PostEntity> posts = new HashMap<>();
        mongo.find(Query.query(Criteria.where("_id").in(chunk.stream().map(Pending::postId).distinct().toList())),
                PostEntity.class).forEach(post -> posts.put(post.getPostId(), post));
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Pending item : chunk) {
            PostEntity post = posts.get(item.postId());
            if (post == null || post.getStatus() != PostStatus.PUBLISHED) continue;
            Map<String, Object> payload = new LinkedHashMap<>(item.extra());
            payload.put("postId", item.postId());
            payload.put("authorId", post.getAuthorId());
            if (post.getSport() != null) payload.put("sport", post.getSport().name());
            Instant at = item.at() == null ? Instant.now() : item.at().atZone(ZoneId.systemDefault()).toInstant();
            futures.add(publisher.send(item.eventType(), item.userId(), payload, at));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);
        return futures.size();
    }
}
