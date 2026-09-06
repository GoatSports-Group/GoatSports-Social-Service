package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.MessageStatus;
import iuh.fit.socialservice.domain.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseCommand {
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String clientMessageId;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private UUID replyToMessageId;
    private List<AttachmentResponseItem> attachments;
    private Boolean deleted;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    private List<ReceiptResponseItem> receipts;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponseItem {
        private UUID attachmentId;
        private String storageKey;
        private String fileName;
        private Long fileSize;
        private AttachmentType type;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptResponseItem {
        private UUID receiptId;
        private UUID userId;
        private MessageStatus status;
        private LocalDateTime occurredAt;
    }
}
