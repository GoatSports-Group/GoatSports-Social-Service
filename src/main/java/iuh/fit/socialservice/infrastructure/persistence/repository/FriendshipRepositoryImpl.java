package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.infrastructure.persistence.mapper.FriendshipEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryImpl implements FriendshipRepository {
    private final JpaFriendshipRepository jpaFriendshipRepository;
    private final FriendshipEntityMapper friendshipEntityMapper;

    @Override
    public Friendship save(Friendship friendship) {
        return friendshipEntityMapper.toDomain(jpaFriendshipRepository.save(friendshipEntityMapper.toEntity(friendship)));
    }

    @Override
    public Optional<Friendship> findById(UUID friendshipId) {
        return jpaFriendshipRepository.findById(friendshipId).map(friendshipEntityMapper::toDomain);
    }

    @Override
    public List<Friendship> findUserFriends(UUID userId, FriendshipStatus status) {
        return jpaFriendshipRepository.findUserFriends(userId, status).stream()
                .map(friendshipEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> findAcceptedOf(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return List.of();
        return jpaFriendshipRepository.findByStatusAndUserIn(FriendshipStatus.ACCEPTED, userIds).stream()
                .map(friendshipEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> findByAddresseeIdAndStatus(UUID addresseeId, FriendshipStatus status) {
        return jpaFriendshipRepository.findByAddresseeIdAndStatus(addresseeId, status).stream()
                .map(friendshipEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> findByRequesterIdAndStatus(UUID requesterId, FriendshipStatus status) {
        return jpaFriendshipRepository.findByRequesterIdAndStatus(requesterId, status).stream()
                .map(friendshipEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Friendship> findBetweenUsers(UUID u1, UUID u2) {
        return jpaFriendshipRepository.findBetweenUsers(u1, u2).map(friendshipEntityMapper::toDomain);
    }

    @Override
    public void deleteById(UUID friendshipId) {
        jpaFriendshipRepository.deleteById(friendshipId);
    }
}
