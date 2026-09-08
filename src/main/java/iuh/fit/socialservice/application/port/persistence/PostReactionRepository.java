package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.ReactionType;

import java.util.Optional;
import java.util.UUID;

public interface PostReactionRepository {
    PostReaction save(PostReaction reaction);

    Optional<PostReaction> find(UUID postId, UUID userId, ReactionType type);

    long count(UUID postId, ReactionType type);

    void delete(UUID postId, UUID userId, ReactionType type);
}
