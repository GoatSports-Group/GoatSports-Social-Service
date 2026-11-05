package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.entity.PlayerCall;
import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.ReportTargetType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nội dung bị báo cáo / khiếu nại, cho admin đọc trước khi xử lý (kể cả khi đã bị ẩn hoặc gỡ).
 * {@code found = false} khi nội dung không còn hoặc loại đó không thuộc mạng xã hội (VENUE, REVIEW).
 * Bài viết có thể không có chữ: ảnh / tệp ({@code attachments}), khoe trận ({@code highlight}), kèo tìm người
 * ({@code playerCall}) hoặc chia sẻ lại bài khác ({@code shared}, chỉ một cấp).
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
        LocalDateTime createdAt,
        List<Attachment> attachments,
        String sport,
        PostHighlight highlight,
        PlayerCall playerCall,
        ModerationTargetResponseCommand shared
) {
    public ModerationTargetResponseCommand(ReportTargetType targetType, UUID targetId, boolean found, UUID authorId,
                                           String content, String status, int attachmentCount, UUID postId,
                                           LocalDateTime createdAt) {
        this(targetType, targetId, found, authorId, content, status, attachmentCount, postId, createdAt, List.of(),
                null, null, null, null);
    }

    /** {@code fileName} chỉ có với tệp trong tin nhắn; ảnh bài viết hiển thị theo {@code storageKey}. */
    public record Attachment(String storageKey, AttachmentType type, String fileName) {
    }

    public static ModerationTargetResponseCommand missing(ReportTargetType type, UUID id) {
        return new ModerationTargetResponseCommand(type, id, false, null, null, null, 0, null, null);
    }
}
