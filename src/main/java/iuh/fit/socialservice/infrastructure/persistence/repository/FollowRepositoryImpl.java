package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.infrastructure.persistence.mapper.FollowEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public org.springframework.data.domain.Page<Follow> findFollowersOf(UUID userId, java.util.Collection<UUID> hidden,
                                                                        org.springframework.data.domain.Pageable pageable) {
        return jpaFollowRepository.findByTargetTypeAndTargetIdAndFollowerIdNotIn(
                FollowTargetType.USER, userId, notEmpty(hidden), newestFirst(pageable)).map(followEntityMapper::toDomain);
    }

    @Override
    public org.springframework.data.domain.Page<Follow> findFollowingOf(UUID userId, java.util.Collection<UUID> hidden,
                                                                        org.springframework.data.domain.Pageable pageable) {
        return jpaFollowRepository.findByFollowerIdAndTargetTypeAndTargetIdNotIn(
                userId, FollowTargetType.USER, notEmpty(hidden), newestFirst(pageable)).map(followEntityMapper::toDomain);
    }

    /** "NOT IN ()" khong hop le o moi CSDL: dung mot id khong ton tai khi khong co ai can an. */
    private static java.util.Collection<UUID> notEmpty(java.util.Collection<UUID> ids) {
        return ids == null || ids.isEmpty() ? java.util.List.of(new UUID(0, 0)) : ids;
    }

    private static org.springframework.data.domain.Pageable newestFirst(org.springframework.data.domain.Pageable pageable) {
        return org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public List<Follow> findByFollowerIdAndTargetType(UUID followerId, FollowTargetType targetType) {
        return jpaFollowRepository.findByFollowerIdAndTargetType(followerId, targetType).stream()
                .map(followEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Follow> findByTargetTypeAndTargetId(FollowTargetType targetType, UUID targetId) {
        return jpaFollowRepository.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .map(followEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countByTargetTypeAndTargetId(FollowTargetType targetType, UUID targetId) {
        return jpaFollowRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }
}
