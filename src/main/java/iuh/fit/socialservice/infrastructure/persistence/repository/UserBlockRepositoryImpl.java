package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.infrastructure.persistence.mapper.UserBlockEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaUserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserBlockRepositoryImpl implements UserBlockRepository {
    private final JpaUserBlockRepository jpaUserBlockRepository;
    private final UserBlockEntityMapper userBlockEntityMapper;

    @Override
    public UserBlock save(UserBlock userBlock) {
        return userBlockEntityMapper.toDomain(jpaUserBlockRepository.save(userBlockEntityMapper.toEntity(userBlock)));
    }

    @Override
    public Optional<UserBlock> findById(UUID blockId) {
        return jpaUserBlockRepository.findById(blockId).map(userBlockEntityMapper::toDomain);
    }

    @Override
    public List<UserBlock> findByBlockerId(UUID blockerId) {
        return jpaUserBlockRepository.findByBlockerId(blockerId).stream()
                .map(userBlockEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserBlock> findByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId) {
        return jpaUserBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, blockedUserId).map(userBlockEntityMapper::toDomain);
    }

    @Override
    public boolean existsByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId) {
        return jpaUserBlockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId);
    }

    @Override
    public void deleteById(UUID blockId) {
        jpaUserBlockRepository.deleteById(blockId);
    }
}
