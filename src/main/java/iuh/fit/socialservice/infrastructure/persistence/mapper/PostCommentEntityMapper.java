package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PostCommentEntityMapper {
    public PostComment toDomain(PostCommentEntity entity) {
        if (entity == null) return null;
        return PostComment.builder()
                .commentId(toUuid(entity.getCommentId()))
                .postId(toUuid(entity.getPostId()))
                .authorId(toUuid(entity.getAuthorId()))
                .parentCommentId(toUuid(entity.getParentCommentId()))
                .content(entity.getContent())
                .mentions(PostEntityMapper.toMentions(entity.getMentions()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PostCommentEntity toEntity(PostComment domain) {
        if (domain == null) return null;
        return PostCommentEntity.builder()
                .commentId(toString(domain.getCommentId()))
                .postId(toString(domain.getPostId()))
                .authorId(toString(domain.getAuthorId()))
                .parentCommentId(toString(domain.getParentCommentId()))
                .content(domain.getContent())
                .mentions(PostEntityMapper.toMentionDocs(domain.getMentions()))
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
