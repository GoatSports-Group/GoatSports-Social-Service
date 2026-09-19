package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.ModerationActionType;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentAppealResponseCommand {
    private UUID appealId;
    private UUID actionId;
    private UUID appellantId;
    private String reason;
    private AppealStatus status;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private ModerationActionType actionType;
    private ReportTargetType targetType;
    private UUID targetId;
}
