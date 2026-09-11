package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.infrastructure.persistence.entity.FollowEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowEntityMapper {
    Follow toDomain(FollowEntity entity);
    FollowEntity toEntity(Follow domain);
}
