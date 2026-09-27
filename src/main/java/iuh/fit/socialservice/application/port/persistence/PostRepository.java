package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.PostSport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);

    Optional<Post> findById(UUID postId);

    List<Post> findAllById(Collection<UUID> postIds);

    Page<Post> findFeed(FeedQuery query, Pageable pageable);

    /** Tac gia co nhieu bai cong khai nhat tu {@code since}, bo qua {@code excludedAuthorIds}. */
    List<AuthorActivity> findActiveAuthors(LocalDateTime since, Collection<UUID> excludedAuthorIds, int limit);

    /**
     * Bo loc bang tin. Quyen xem (cong khai / ban be / cua minh) va danh sach chan luon ap dung;
     * {@code sport} va {@code authorId} chi thu hep them.
     */
    record FeedQuery(
            UUID viewerId,
            Set<UUID> friendIds,
            Set<UUID> followingIds,
            Set<UUID> blockedUserIds,
            boolean followingOnly,
            PostSport sport,
            UUID authorId
    ) {
    }

    record AuthorActivity(UUID authorId, long postCount) {
    }
}
