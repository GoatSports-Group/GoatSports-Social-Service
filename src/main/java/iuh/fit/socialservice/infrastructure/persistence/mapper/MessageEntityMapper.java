package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.domain.entity.MessageAttachment;
import iuh.fit.socialservice.domain.entity.MessageReceipt;
import iuh.fit.socialservice.infrastructure.persistence.entity.MessageAttachmentDoc;
import iuh.fit.socialservice.infrastructure.persistence.entity.MessageEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.MessageReceiptDoc;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class MessageEntityMapper {

    public Message toDomain(MessageEntity entity) {
        if (entity == null) return null;
        return Message.builder()
                .messageId(toUuid(entity.getMessageId()))
                .conversationId(toUuid(entity.getConversationId()))
                .senderId(toUuid(entity.getSenderId()))
                .clientMessageId(entity.getClientMessageId())
                .content(entity.getContent())
                .type(entity.getType())
                .status(entity.getStatus())
                .replyToMessageId(toUuid(entity.getReplyToMessageId()))
                .deleted(entity.getDeleted())
                .sentAt(entity.getSentAt())
                .editedAt(entity.getEditedAt())
                .attachments(toDomainAttachments(entity.getAttachments()))
                .receipts(toDomainReceipts(entity.getReceipts()))
                .build();
    }

    public MessageEntity toEntity(Message domain) {
        if (domain == null) return null;
        return MessageEntity.builder()
                .messageId(toString(domain.getMessageId()))
                .conversationId(toString(domain.getConversationId()))
                .senderId(toString(domain.getSenderId()))
                .clientMessageId(domain.getClientMessageId())
                .content(domain.getContent())
                .type(domain.getType())
                .status(domain.getStatus())
                .replyToMessageId(toString(domain.getReplyToMessageId()))
                .deleted(domain.getDeleted())
                .sentAt(domain.getSentAt())
                .editedAt(domain.getEditedAt())
                .attachments(toAttachmentDocs(domain.getAttachments()))
                .receipts(toReceiptDocs(domain.getReceipts()))
                .build();
    }

    private List<MessageAttachment> toDomainAttachments(List<MessageAttachmentDoc> docs) {
        if (docs == null) return Collections.emptyList();
        return docs.stream().map(doc -> MessageAttachment.builder()
                .attachmentId(toUuid(doc.getAttachmentId()))
                .storageKey(doc.getStorageKey())
                .type(doc.getType())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .build()).toList();
    }

    private List<MessageAttachmentDoc> toAttachmentDocs(List<MessageAttachment> attachments) {
        if (attachments == null) return Collections.emptyList();
        return attachments.stream().map(attachment -> MessageAttachmentDoc.builder()
                .attachmentId(toString(attachment.getAttachmentId()))
                .storageKey(attachment.getStorageKey())
                .type(attachment.getType())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .build()).toList();
    }

    private List<MessageReceipt> toDomainReceipts(List<MessageReceiptDoc> docs) {
        if (docs == null) return Collections.emptyList();
        return docs.stream().map(doc -> MessageReceipt.builder()
                .receiptId(toUuid(doc.getReceiptId()))
                .userId(toUuid(doc.getUserId()))
                .status(doc.getStatus())
                .occurredAt(doc.getOccurredAt())
                .build()).toList();
    }

    private List<MessageReceiptDoc> toReceiptDocs(List<MessageReceipt> receipts) {
        if (receipts == null) return Collections.emptyList();
        return receipts.stream().map(receipt -> MessageReceiptDoc.builder()
                .receiptId(toString(receipt.getReceiptId()))
                .userId(toString(receipt.getUserId()))
                .status(receipt.getStatus())
                .occurredAt(receipt.getOccurredAt())
                .build()).toList();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
