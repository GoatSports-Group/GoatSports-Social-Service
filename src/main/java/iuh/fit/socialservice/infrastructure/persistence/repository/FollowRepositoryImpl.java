package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.infrastructure.persistence.mapper.FollowEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepository {
    private final JpaFollowRepository jpaFollowRepository;
    private final FollowEntityMapper followEntityMapper;

    @Override
    public Follow save(Follow follow) {
        return followEntityMapper.toDomain(jpaFollowRepository.save(followEntityMapper.toEntity(follow)));
    }

    @Override
    public Optional<Follow> findByFollowerIdAndTargetTypeAndTargetId(
            UUID followerId,
            FollowTargetType targetType,
            UUID targetId
    ) {
        return jpaFollowRepository.findByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId)
                .map(followEntityMapper::toDomain);
    }

    @Override
    public void deleteById(UUID followId) {
        jpaFollowRepository.deleteById(followId);
    }
}
