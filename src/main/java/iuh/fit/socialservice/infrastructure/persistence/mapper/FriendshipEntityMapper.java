package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.infrastructure.persistence.entity.FriendshipEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FriendshipEntityMapper {
    Friendship toDomain(FriendshipEntity entity);
    FriendshipEntity toEntity(Friendship domain);
}
