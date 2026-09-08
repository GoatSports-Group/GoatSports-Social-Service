package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostAttachment;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostAttachmentDoc;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class PostEntityMapper {
    public Post toDomain(PostEntity entity) {
        if (entity == null) return null;
        return Post.builder()
                .postId(toUuid(entity.getPostId()))
                .authorId(toUuid(entity.getAuthorId()))
                .content(entity.getContent())
                .visibility(entity.getVisibility())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .publishedAt(entity.getPublishedAt())
                .attachments(entity.getAttachments() == null ? Collections.emptyList()
                        : entity.getAttachments().stream().map(this::toDomain).toList())
                .build();
    }

    public PostEntity toEntity(Post domain) {
        if (domain == null) return null;
        return PostEntity.builder()
                .postId(toString(domain.getPostId()))
                .authorId(toString(domain.getAuthorId()))
                .content(domain.getContent())
                .visibility(domain.getVisibility())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .publishedAt(domain.getPublishedAt())
                .attachments(domain.getAttachments() == null ? Collections.emptyList()
                        : domain.getAttachments().stream().map(this::toDocument).toList())
                .build();
    }

    private PostAttachment toDomain(PostAttachmentDoc document) {
        return PostAttachment.builder()
                .attachmentId(toUuid(document.getAttachmentId()))
                .storageKey(document.getStorageKey())
                .type(document.getType())
                .displayOrder(document.getDisplayOrder())
                .build();
    }

    private PostAttachmentDoc toDocument(PostAttachment attachment) {
        return PostAttachmentDoc.builder()
                .attachmentId(toString(attachment.getAttachmentId()))
                .storageKey(attachment.getStorageKey())
                .type(attachment.getType())
                .displayOrder(attachment.getDisplayOrder())
                .build();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
