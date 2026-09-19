package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.ModerationActionRepository;
import iuh.fit.socialservice.domain.entity.ModerationAction;
import iuh.fit.socialservice.infrastructure.persistence.entity.ModerationActionEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaContentReportRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaModerationActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ModerationActionRepositoryImpl implements ModerationActionRepository {
    private final JpaModerationActionRepository jpaRepository;
    private final JpaContentReportRepository jpaReportRepository;

    @Override
    @Transactional
    public ModerationAction save(ModerationAction action) {
        ModerationActionEntity entity = ModerationActionEntity.builder()
                .actionId(action.getActionId())
                .report(jpaReportRepository.getReferenceById(action.getReportId()))
                .moderatorId(action.getModeratorId())
                .actionType(action.getActionType())
                .reason(action.getReason())
                .actionAt(action.getActionAt())
                .createdAt(action.getCreatedAt())
                .createdBy(action.getCreatedBy())
                .updatedAt(action.getUpdatedAt())
                .updatedBy(action.getUpdatedBy())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModerationAction> findById(UUID actionId) {
        return jpaRepository.findById(actionId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationAction> findByReportId(UUID reportId) {
        return jpaRepository.findByReport_ReportIdOrderByActionAtAsc(reportId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ModerationAction toDomain(ModerationActionEntity entity) {
        return ModerationAction.builder()
                .actionId(entity.getActionId())
                .reportId(entity.getReport().getReportId())
                .moderatorId(entity.getModeratorId())
                .actionType(entity.getActionType())
                .reason(entity.getReason())
                .actionAt(entity.getActionAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
