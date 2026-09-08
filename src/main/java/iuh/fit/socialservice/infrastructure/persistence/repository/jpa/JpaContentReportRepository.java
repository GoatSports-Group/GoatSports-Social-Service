package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import iuh.fit.socialservice.infrastructure.persistence.entity.ContentReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface JpaContentReportRepository extends JpaRepository<ContentReportEntity, UUID> {
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    );

    Page<ContentReportEntity> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);
}
