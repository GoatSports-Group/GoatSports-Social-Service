package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.ModerationActionType;
import iuh.fit.socialservice.domain.enums.ReportTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Một quyết định kiểm duyệt nhắm vào nội dung (hoặc tài khoản) của người đang xem. Không lộ người báo cáo.
 * {@code appealable}: còn khiếu nại được (đúng loại, là quyết định mới nhất của báo cáo, chưa khiếu nại).
 */
public record ModerationDecisionResponseCommand(
        UUID actionId,
        ModerationActionType actionType,
        String reason,
        LocalDateTime actionAt,
        ReportTargetType targetType,
        UUID targetId,
        String contentSnippet,
        String contentStatus,
        boolean appealable,
        Appeal appeal
) {
    public record Appeal(UUID appealId, AppealStatus status, String reason, LocalDateTime createdAt,
                         LocalDateTime reviewedAt) {
    }
}
