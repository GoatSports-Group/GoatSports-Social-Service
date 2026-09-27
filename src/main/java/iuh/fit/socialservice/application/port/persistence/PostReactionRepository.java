package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.ReactionType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PostReactionRepository {
    PostReaction save(PostReaction reaction);

    Optional<PostReaction> find(UUID postId, UUID userId, ReactionType type);

    long count(UUID postId, ReactionType type);

    void delete(UUID postId, UUID userId, ReactionType type);

    /** Moi nhat truoc. */
    Page<PostReaction> findByUser(UUID userId, ReactionType type, Pageable pageable);
}
