package iuh.fit.socialservice.domain.service;

import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.PostVisibility;

import java.util.Set;
import java.util.UUID;

/** Ai duoc xem mot bai viet. Cung quy tac voi bo loc bang tin trong PostRepositoryImpl. */
public final class PostVisibilityPolicy {
    private PostVisibilityPolicy() {
    }

    /**
     * Chan hai chieu xet truoc nhanh PUBLIC: neu khong, nguoi bi chan van thich / binh luan duoc bai
     * cong khai cua nguoi da chan minh bang cach goi thang theo id.
     *
     * @param blockedIds nguoi ma {@code viewerId} chan hoac bi chan boi
     */
    public static boolean canView(Post post, UUID viewerId, Set<UUID> friendIds, Set<UUID> blockedIds) {
        if (viewerId != null && viewerId.equals(post.getAuthorId())) return true;
        if (viewerId != null && blockedIds.contains(post.getAuthorId())) return false;
        if (post.getVisibility() == PostVisibility.PUBLIC) return true;
        if (viewerId == null) return false;
        return post.getVisibility() == PostVisibility.FRIENDS && friendIds.contains(post.getAuthorId());
    }
}
