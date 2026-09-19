package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.ContentAppealRepository;
import iuh.fit.socialservice.domain.entity.ContentAppeal;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.ContentAppealEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaContentAppealRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaModerationActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ContentAppealRepositoryImpl implements ContentAppealRepository {
    private final JpaContentAppealRepository jpaRepository;
    private final JpaModerationActionRepository jpaActionRepository;

    @Override
    @Transactional
    public ContentAppeal save(ContentAppeal appeal) {
        ContentAppealEntity entity = ContentAppealEntity.builder()
                .appealId(appeal.getAppealId())
                .action(jpaActionRepository.getReferenceById(appeal.getActionId()))
                .appellantId(appeal.getAppellantId())
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .reviewedBy(appeal.getReviewedBy())
                .reviewedAt(appeal.getReviewedAt())
                .createdAt(appeal.getCreatedAt())
                .createdBy(appeal.getCreatedBy())
                .updatedAt(appeal.getUpdatedAt())
                .updatedBy(appeal.getUpdatedBy())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContentAppeal> findById(UUID appealId) {
        return jpaRepository.findById(appealId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByActionId(UUID actionId) {
        return jpaRepository.existsByAction_ActionId(actionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentAppeal> findByStatusIn(Collection<AppealStatus> statuses, Pageable pageable) {
        return jpaRepository.findByStatusInOrderByCreatedAtAsc(statuses, pageable).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentAppeal> findByAppellantId(UUID appellantId, Pageable pageable) {
        return jpaRepository.findByAppellantIdOrderByCreatedAtDesc(appellantId, pageable).map(this::toDomain);
    }

    private ContentAppeal toDomain(ContentAppealEntity entity) {
        return ContentAppeal.builder()
                .appealId(entity.getAppealId())
                .actionId(entity.getAction().getActionId())
                .appellantId(entity.getAppellantId())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
