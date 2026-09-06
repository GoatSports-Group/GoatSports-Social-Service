package iuh.fit.socialservice.application.dto.request;

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
