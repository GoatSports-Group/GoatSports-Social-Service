package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.infrastructure.persistence.entity.UserBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserBlockRepository extends JpaRepository<UserBlockEntity, UUID> {
    List<UserBlockEntity> findByBlockerId(UUID blockerId);
    Optional<UserBlockEntity> findByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);
    boolean existsByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);
}
