package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.PostShare;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PostShareEntityMapper {
    public PostShare toDomain(PostShareEntity entity) {
        if (entity == null) return null;
        return PostShare.builder()
                .shareId(toUuid(entity.getShareId()))
                .postId(toUuid(entity.getPostId()))
                .userId(toUuid(entity.getUserId()))
                .caption(entity.getCaption())
                .sharedAt(entity.getSharedAt())
                .build();
    }

    public PostShareEntity toEntity(PostShare domain) {
        if (domain == null) return null;
        return PostShareEntity.builder()
                .shareId(toString(domain.getShareId()))
                .postId(toString(domain.getPostId()))
                .userId(toString(domain.getUserId()))
                .caption(domain.getCaption())
                .sharedAt(domain.getSharedAt())
                .build();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
