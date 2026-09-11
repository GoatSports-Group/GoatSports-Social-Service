package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.infrastructure.persistence.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFollowRepository extends JpaRepository<FollowEntity, UUID> {
    Optional<FollowEntity> findByFollowerIdAndTargetTypeAndTargetId(
            UUID followerId,
            FollowTargetType targetType,
            UUID targetId
    );
}
