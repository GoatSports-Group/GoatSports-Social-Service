package iuh.fit.socialservice.domain.entity;

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
public class Message {
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String clientMessageId;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private UUID replyToMessageId;
    private Boolean deleted;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    private List<MessageAttachment> attachments;
    private List<MessageReceipt> receipts;
}
