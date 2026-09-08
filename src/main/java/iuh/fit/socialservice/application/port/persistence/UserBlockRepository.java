package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.UserBlock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository {
    UserBlock save(UserBlock userBlock);
    Optional<UserBlock> findById(UUID blockId);
    List<UserBlock> findByBlockerId(UUID blockerId);
    List<UserBlock> findRelatedToUser(UUID userId);
    Optional<UserBlock> findByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);
    boolean existsByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);
    void deleteById(UUID blockId);
}
