package iuh.fit.socialservice.infrastructure.external.adapter;

import iuh.fit.socialservice.application.port.external.PostRanker;
import iuh.fit.socialservice.infrastructure.external.client.AiPersonalizationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI lỗi thì trả rỗng (bảng tin giữ thứ tự mới nhất) và nghỉ gọi một phút, để mỗi lượt mở Khám phá không phải chờ
 * hết timeout trong lúc ai-service đang sập.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostRankerAdapter implements PostRanker {
    static final Duration PAUSE = Duration.ofMinutes(1);

    private final AiPersonalizationClient client;
    private volatile Instant pausedUntil = Instant.EPOCH;

    @Override
    public Map<UUID, Double> rank(UUID viewerId, List<Candidate> candidates) {
        if (candidates.isEmpty() || Instant.now().isBefore(pausedUntil)) return Map.of();
        try {
            List<AiPersonalizationClient.PostScore> scores = client.rankPosts(new AiPersonalizationClient.RankRequest(
                    viewerId, candidates.stream().map(PostRankerAdapter::toRequest).toList()));
            Map<UUID, Double> byPost = new HashMap<>();
            if (scores != null) {
                scores.stream()
                        .filter(score -> score.postId() != null && score.score() != null)
                        .forEach(score -> byPost.put(score.postId(), score.score()));
            }
            return byPost;
        } catch (RuntimeException exception) {
            pausedUntil = Instant.now().plus(PAUSE);
            log.warn("Feed ranking unavailable, showing newest posts for {}s: {}", PAUSE.toSeconds(),
                    exception.getMessage());
            return Map.of();
        }
    }

    private static AiPersonalizationClient.Candidate toRequest(Candidate candidate) {
        // Giờ đăng lưu theo giờ máy chủ (LocalDateTime): gửi kèm múi giờ để ai-service tính độ mới đúng.
        return new AiPersonalizationClient.Candidate(candidate.postId(), candidate.authorId(),
                candidate.sport() == null ? null : candidate.sport().name(),
                candidate.likeCount(), candidate.commentCount(),
                candidate.publishedAt() == null ? null
                        : candidate.publishedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
