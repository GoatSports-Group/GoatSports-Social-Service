package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.MessageStatus;
import iuh.fit.socialservice.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "conv_sent_idx", def = "{'conversationId': 1, 'sentAt': -1}"),
        @CompoundIndex(name = "sender_client_message_idx", def = "{'senderId': 1, 'clientMessageId': 1}", unique = true, sparse = true)
})
public class MessageEntity {

    @Id
    private String messageId;

    @Indexed
    private String conversationId;

    @Indexed
    private String senderId;

    private String clientMessageId;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private String replyToMessageId;
    private Boolean deleted;

    @Indexed
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;

    @Builder.Default
    private List<MessageAttachmentDoc> attachments = new ArrayList<>();

    @Builder.Default
    private List<MessageReceiptDoc> receipts = new ArrayList<>();
}
