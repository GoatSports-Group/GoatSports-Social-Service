package iuh.fit.socialservice.application.port.external;

import java.util.Map;
import java.util.UUID;

/**
 * Phat su kien cong dong cho notification-service. Nguoi goi da loc: khong gui cho chinh minh,
 * khong gui qua mot quan he chan. Loi phat khong duoc lam hong nghiep vu goc.
 */
public interface SocialEventPublisher {
    String POST_LIKED = "social.post.liked.v1";
    String POST_COMMENTED = "social.post.commented.v1";
    String COMMENT_REPLIED = "social.comment.replied.v1";
    String POST_SHARED = "social.post.shared.v1";
    String USER_FOLLOWED = "social.user.followed.v1";
    String POST_MENTIONED = "social.post.mentioned.v1";
    String COMMENT_MENTIONED = "social.comment.mentioned.v1";
    String FRIEND_REQUESTED = "social.friend.requested.v1";
    String FRIEND_ACCEPTED = "social.friend.accepted.v1";

    /**
     * @param dedupeKey khoa cua su kien "chi bao mot lan" (vd. thich bai X boi nguoi Y). Cung khoa → cung
     *                  eventId, nen notification-service bo qua lan sau. null = moi lan la mot su kien moi.
     */
    void publish(String eventType, UUID receiverId, UUID actorId, UUID aggregateId, Map<String, String> payload,
                 String dedupeKey);
}
