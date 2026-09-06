package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository {
    Friendship save(Friendship friendship);
    Optional<Friendship> findById(UUID friendshipId);
    List<Friendship> findUserFriends(UUID userId, FriendshipStatus status);
    List<Friendship> findByAddresseeIdAndStatus(UUID addresseeId, FriendshipStatus status);
    List<Friendship> findByRequesterIdAndStatus(UUID requesterId, FriendshipStatus status);
    Optional<Friendship> findBetweenUsers(UUID u1, UUID u2);
    void deleteById(UUID friendshipId);
}
