package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import iuh.fit.socialservice.infrastructure.persistence.entity.ContentReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    /** Mức AI cao lên đầu; chưa có gợi ý thì xếp sau, cũ trước. */
    @Query("select report from ContentReportEntity report where report.status in :statuses "
            + "order by report.aiSeverity desc nulls last, report.createdAt asc")
    Page<ContentReportEntity> findQueue(@Param("statuses") Collection<ReportStatus> statuses, Pageable pageable);

    /** Báo cáo đang mở về nội dung có chữ, AI chưa đọc và chưa hết lượt thử. */
    @Query(value = """
            select * from content_reports
            where status in ('PENDING', 'REVIEWING') and target_type in ('POST', 'COMMENT', 'MESSAGE')
              and ai_checked_at is null and ai_attempts < :maxAttempts
            order by created_at
            limit :limit""", nativeQuery = true)
    List<ContentReportEntity> findAwaitingAiVerdict(@Param("maxAttempts") int maxAttempts, @Param("limit") int limit);

    @Modifying
    @Query(value = """
            update content_reports set ai_label = :label, ai_severity = :severity, ai_reason = :reason,
              ai_checked_at = :checkedAt
            where report_id = :reportId""", nativeQuery = true)
    void saveAiVerdict(
            @Param("reportId") UUID reportId,
            @Param("label") String label,
            @Param("severity") Integer severity,
            @Param("reason") String reason,
            @Param("checkedAt") LocalDateTime checkedAt
    );

    @Modifying
    @Query(value = "update content_reports set ai_attempts = ai_attempts + :attempts where report_id = :reportId",
            nativeQuery = true)
    void addAiAttempts(@Param("reportId") UUID reportId, @Param("attempts") int attempts);
}
