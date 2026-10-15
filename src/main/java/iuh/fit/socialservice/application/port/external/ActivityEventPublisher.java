package iuh.fit.socialservice.application.port.external;

import java.util.Map;
import java.util.UUID;

/**
 * Phát hành vi của người dùng với bài viết (thích, lưu, bình luận, chia sẻ) sang ai-service để xếp hạng tab
 * "Khám phá", qua topic {@code user-activity-topic}. Khác {@link SocialEventPublisher} (thông báo cho người nhận):
 * ở đây phát cả lúc bỏ thích / xóa bình luận, để ai-service gỡ tín hiệu. Lỗi phát không làm hỏng nghiệp vụ gốc.
 */
public interface ActivityEventPublisher {
    String POST_REACTION = "activity.post.reaction.v1";
    String POST_COMMENT = "activity.post.comment.v1";
    String POST_SHARED = "activity.post.shared.v1";

    void publish(String eventType, UUID userId, Map<String, Object> payload);
}
