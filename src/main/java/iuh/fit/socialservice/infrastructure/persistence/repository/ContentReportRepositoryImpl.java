package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import iuh.fit.socialservice.infrastructure.persistence.entity.ContentReportEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaContentReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ContentReportRepositoryImpl implements ContentReportRepository {
    private final JpaContentReportRepository jpaRepository;

    @Override
    @Transactional
    public ContentReport save(ContentReport report) {
        ContentReportEntity entity = ContentReportEntity.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporterId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .evidence(new ArrayList<>(report.getEvidence()))
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .createdBy(report.getCreatedBy())
                .updatedAt(report.getUpdatedAt())
                .updatedBy(report.getUpdatedBy())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public boolean existsOpenReport(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    ) {
        return jpaRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                reporterId, targetType, targetId, statuses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentReport> findByReporterId(UUID reporterId, Pageable pageable) {
        return jpaRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable).map(this::toDomain);
    }

    private ContentReport toDomain(ContentReportEntity entity) {
        return ContentReport.builder()
                .reportId(entity.getReportId())
                .reporterId(entity.getReporterId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .reason(entity.getReason())
                .evidence(new ArrayList<>(entity.getEvidence()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
