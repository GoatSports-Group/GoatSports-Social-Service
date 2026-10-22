package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.infrastructure.persistence.entity.FollowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFollowRepository extends JpaRepository<FollowEntity, UUID> {
    Optional<FollowEntity> findByFollowerIdAndTargetTypeAndTargetId(
            UUID followerId,
            FollowTargetType targetType,
            UUID targetId
    );

    List<FollowEntity> findByFollowerIdAndTargetType(UUID followerId, FollowTargetType targetType);

    List<FollowEntity> findByTargetTypeAndTargetId(FollowTargetType targetType, UUID targetId);

    long countByTargetTypeAndTargetId(FollowTargetType targetType, UUID targetId);

    Page<FollowEntity> findByTargetTypeAndTargetIdAndFollowerIdNotIn(
            FollowTargetType targetType, UUID targetId, Collection<UUID> hidden,
            Pageable pageable);

    Page<FollowEntity> findByFollowerIdAndTargetTypeAndTargetIdNotIn(
            UUID followerId, FollowTargetType targetType, Collection<UUID> hidden,
            Pageable pageable);

    @Modifying
    @Query("delete from FollowEntity f where f.followerId = :userId or f.targetId = :userId")
    int deleteAllOf(@Param("userId") UUID userId);
}
