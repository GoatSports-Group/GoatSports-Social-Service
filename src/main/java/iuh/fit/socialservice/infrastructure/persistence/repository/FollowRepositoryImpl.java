package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.infrastructure.persistence.mapper.FollowEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
    public Page<Follow> findFollowersOf(UUID userId, Collection<UUID> hidden,
                                                                        Pageable pageable) {
        return jpaFollowRepository.findByTargetTypeAndTargetIdAndFollowerIdNotIn(
                FollowTargetType.USER, userId, notEmpty(hidden), newestFirst(pageable)).map(followEntityMapper::toDomain);
    }

    @Override
    public Page<Follow> findFollowingOf(UUID userId, Collection<UUID> hidden,
                                                                        Pageable pageable) {
        return jpaFollowRepository.findByFollowerIdAndTargetTypeAndTargetIdNotIn(
                userId, FollowTargetType.USER, notEmpty(hidden), newestFirst(pageable)).map(followEntityMapper::toDomain);
    }

    /** "NOT IN ()" khong hop le o moi CSDL: dung mot id khong ton tai khi khong co ai can an. */
    private static Collection<UUID> notEmpty(Collection<UUID> ids) {
        return ids == null || ids.isEmpty() ? List.of(new UUID(0, 0)) : ids;
    }

    private static Pageable newestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
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
