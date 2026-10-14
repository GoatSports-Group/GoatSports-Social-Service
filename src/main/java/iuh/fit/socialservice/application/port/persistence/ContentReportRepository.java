package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.AiVerdict;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentReportRepository {
    ContentReport save(ContentReport report);

    Optional<ContentReport> findById(UUID reportId);

    boolean existsOpenReport(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    );

    Page<ContentReport> findByReporterId(UUID reporterId, Pageable pageable);

    Page<ContentReport> findQueue(Collection<ReportStatus> statuses, Pageable pageable);

    List<ContentReport> findAwaitingAiVerdict(int maxAttempts, int limit);

    void saveAiVerdict(UUID reportId, AiVerdict verdict);

    void addAiAttempts(UUID reportId, int attempts);
}
