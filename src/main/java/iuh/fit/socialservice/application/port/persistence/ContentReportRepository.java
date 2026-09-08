package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.UUID;

public interface ContentReportRepository {
    ContentReport save(ContentReport report);

    boolean existsOpenReport(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    );

    Page<ContentReport> findByReporterId(UUID reporterId, Pageable pageable);
}
