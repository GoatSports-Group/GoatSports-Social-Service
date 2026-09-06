package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.infrastructure.persistence.entity.UserBlockEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserBlockEntityMapper {
    UserBlock toDomain(UserBlockEntity entity);
    UserBlockEntity toEntity(UserBlock domain);
}
