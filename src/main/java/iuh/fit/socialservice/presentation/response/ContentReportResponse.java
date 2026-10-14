package iuh.fit.socialservice.presentation.response;

import iuh.fit.socialservice.domain.enums.ModerationActionType;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import iuh.fit.socialservice.domain.entity.AiVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReportResponse {
    private UUID reportId;
    private UUID reporterId;
    private ReportTargetType targetType;
    private UUID targetId;
    private String reason;
    private List<String> evidence;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ActionItem> actions;
    /** Gợi ý của AI, chỉ trả cho admin; null khi AI chưa đọc hoặc không đọc được. */
    private AiVerdict ai;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private UUID actionId;
        private UUID moderatorId;
        private ModerationActionType actionType;
        private String reason;
        private LocalDateTime actionAt;
    }
}
