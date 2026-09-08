package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PostReactionEntityMapper {
    public PostReaction toDomain(PostReactionEntity entity) {
        if (entity == null) return null;
        return PostReaction.builder()
                .reactionId(toUuid(entity.getReactionId()))
                .postId(toUuid(entity.getPostId()))
                .userId(toUuid(entity.getUserId()))
                .type(entity.getType())
                .reactedAt(entity.getReactedAt())
                .build();
    }

    public PostReactionEntity toEntity(PostReaction domain) {
        if (domain == null) return null;
        return PostReactionEntity.builder()
                .reactionId(toString(domain.getReactionId()))
                .postId(toString(domain.getPostId()))
                .userId(toString(domain.getUserId()))
                .type(domain.getType())
                .reactedAt(domain.getReactedAt())
                .build();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
