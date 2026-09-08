package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);

    Optional<Post> findById(UUID postId);

    Page<Post> findFeed(UUID viewerId, Set<UUID> friendIds, Set<UUID> blockedUserIds, Pageable pageable);
}
