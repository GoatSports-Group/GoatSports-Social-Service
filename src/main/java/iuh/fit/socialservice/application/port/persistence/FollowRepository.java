package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;

import java.util.Optional;
import java.util.UUID;

public interface FollowRepository {
    Follow save(Follow follow);
    Optional<Follow> findByFollowerIdAndTargetTypeAndTargetId(
            UUID followerId,
            FollowTargetType targetType,
            UUID targetId
    );
    void deleteById(UUID followId);
}
