package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.infrastructure.persistence.entity.UserBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserBlockRepository extends JpaRepository<UserBlockEntity, UUID> {
    List<UserBlockEntity> findByBlockerId(UUID blockerId);
    @Query("SELECT b FROM UserBlockEntity b WHERE b.blockerId = :userId OR b.blockedUserId = :userId")
    List<UserBlockEntity> findRelatedToUser(@Param("userId") UUID userId);
    Optional<UserBlockEntity> findByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);
    boolean existsByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from UserBlockEntity b where b.blockerId = :userId or b.blockedUserId = :userId")
    int deleteAllOf(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
