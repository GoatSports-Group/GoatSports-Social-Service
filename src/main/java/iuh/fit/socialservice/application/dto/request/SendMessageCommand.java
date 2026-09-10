package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.MessageType;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import lombok.*;

import java.util.UUID;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageCommand {
    private UUID conversationId;
    private UUID senderId;
    private String clientMessageId;
    private String content;
    private MessageType type;
    private UUID replyToMessageId;
    private List<AttachmentItem> attachments;

    public void validate() throws ServiceException {
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (!hasContent && !hasAttachments) {
            throw new ServiceException("Tin nhắn phải có nội dung hoặc tệp đính kèm.");
        }
        if (hasAttachments && attachments.stream().anyMatch(item -> item == null
                || item.storageKey == null || item.storageKey.isBlank() || item.type == null)) {
            throw new ServiceException("Thông tin tệp đính kèm không hợp lệ.");
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItem {
        private UUID attachmentId;
        private String storageKey;
        private AttachmentType type;
        private String fileName;
        private Long fileSize;
    }
}
