package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
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

    List<Follow> findByFollowerIdAndTargetType(UUID followerId, FollowTargetType targetType);

    List<Follow> findByTargetTypeAndTargetId(FollowTargetType targetType, UUID targetId);

    long countByTargetTypeAndTargetId(FollowTargetType targetType, UUID targetId);

    Page<Follow> findFollowersOf(UUID userId, Collection<UUID> hidden,
                                                                 Pageable pageable);

    Page<Follow> findFollowingOf(UUID userId, Collection<UUID> hidden,
                                                                 Pageable pageable);
}
