package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.enums.ReportTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nội dung bị báo cáo / khiếu nại, cho admin đọc trước khi xử lý (kể cả khi đã bị ẩn hoặc gỡ).
 * {@code found = false} khi nội dung không còn hoặc loại đó không thuộc mạng xã hội (VENUE, REVIEW).
 */
public record ModerationTargetResponseCommand(
        ReportTargetType targetType,
        UUID targetId,
        boolean found,
        UUID authorId,
        String content,
        String status,
        int attachmentCount,
        UUID postId,
        LocalDateTime createdAt
) {
    public static ModerationTargetResponseCommand missing(ReportTargetType type, UUID id) {
        return new ModerationTargetResponseCommand(type, id, false, null, null, null, 0, null, null);
    }
}
