package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "content_reports",
        indexes = {
                @Index(name = "idx_content_reports_reporter", columnList = "reporter_id"),
                @Index(name = "idx_content_reports_target", columnList = "target_type,target_id"),
                @Index(name = "idx_content_reports_status", columnList = "status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReportEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "report_id", columnDefinition = "UUID")
    private UUID reportId;

    @Column(name = "reporter_id", nullable = false, columnDefinition = "UUID")
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "UUID")
    private UUID targetId;

    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "content_report_evidence",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @OrderColumn(name = "display_order")
    @Column(name = "storage_key", nullable = false, length = 1000)
    @Builder.Default
    private List<String> evidence = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ModerationActionEntity> actions = new ArrayList<>();
}
