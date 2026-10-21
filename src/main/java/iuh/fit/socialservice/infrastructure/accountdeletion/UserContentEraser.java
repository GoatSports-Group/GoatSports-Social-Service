package iuh.fit.socialservice.infrastructure.accountdeletion;

import iuh.fit.socialservice.infrastructure.external.client.StorageClient;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostAttachmentDoc;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaConversationRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFriendshipRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaUserBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Xóa hẳn nội dung của một tài khoản đã bị ẩn danh hóa: bài viết (kể cả kèo "Tìm người chơi") cùng bình luận, lượt
 * thích, lượt chia sẻ trên đó; bình luận và lượt thích của người đó ở bài người khác; bạn bè, theo dõi, chặn. Rời mọi
 * nhóm chat nhưng giữ tin nhắn 1-1 để người kia còn lịch sử (tên hiển thị thành "Người dùng đã xóa").
 * Mọi bước đều xóa theo điều kiện nên chạy lại vẫn an toàn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContentEraser {
    private final MongoTemplate mongo;
    private final JpaFriendshipRepository friendships;
    private final JpaFollowRepository follows;
    private final JpaUserBlockRepository blocks;
    private final JpaConversationRepository conversations;
    private final StorageClient storageClient;

    @Transactional
    public void erase(UUID userId) {
        String id = userId.toString();
        List<PostEntity> posts = mongo.find(query(where("authorId").is(id)), PostEntity.class);
        List<String> postIds = posts.stream().map(PostEntity::getPostId).toList();
        List<String> commentIds = mongo.find(query(where("authorId").is(id)), PostCommentEntity.class).stream()
                .map(PostCommentEntity::getCommentId).toList();

        mongo.remove(query(new Criteria().orOperator(where("authorId").is(id), where("postId").in(postIds),
                where("parentCommentId").in(commentIds))), PostCommentEntity.class);
        mongo.remove(query(new Criteria().orOperator(where("userId").is(id), where("postId").in(postIds))),
                PostReactionEntity.class);
        mongo.remove(query(new Criteria().orOperator(where("userId").is(id), where("postId").in(postIds))),
                PostShareEntity.class);
        mongo.remove(query(where("authorId").is(id)), PostEntity.class);

        friendships.deleteAllOf(userId);
        follows.deleteAllOf(userId);
        blocks.deleteAllOf(userId);
        conversations.leaveGroupConversations(userId, LocalDateTime.now());

        posts.stream().flatMap(post -> post.getAttachments().stream())
                .map(PostAttachmentDoc::getStorageKey).filter(Objects::nonNull)
                .forEach(key -> deleteFile(key, userId));
        log.info("Erased {} posts and {} comments of deleted user {}", posts.size(), commentIds.size(), userId);
    }

    private void deleteFile(String key, UUID userId) {
        try {
            storageClient.delete(key);
        } catch (Exception e) {
            // ponytail: storage lỗi thì ảnh bài viết thành file mồ côi; bài đã xóa nên không ai còn link tới.
            log.warn("Could not delete post attachment {} of user {}: {}", key, userId, e.getMessage());
        }
    }
}
