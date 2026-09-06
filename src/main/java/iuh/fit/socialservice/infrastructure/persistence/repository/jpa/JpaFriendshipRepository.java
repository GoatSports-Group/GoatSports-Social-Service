package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.FriendshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {
    @Query("SELECT f FROM FriendshipEntity f WHERE (f.requesterId = :userId OR f.addresseeId = :userId) AND f.status = :status")
    List<FriendshipEntity> findUserFriends(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    List<FriendshipEntity> findByAddresseeIdAndStatus(UUID addresseeId, FriendshipStatus status);
    List<FriendshipEntity> findByRequesterIdAndStatus(UUID requesterId, FriendshipStatus status);

    @Query("SELECT f FROM FriendshipEntity f WHERE (f.requesterId = :u1 AND f.addresseeId = :u2) OR (f.requesterId = :u2 AND f.addresseeId = :u1)")
    Optional<FriendshipEntity> findBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
