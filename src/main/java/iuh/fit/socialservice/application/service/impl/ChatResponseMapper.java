package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.Message;

import java.util.List;
import java.util.Optional;

/** Dựng response cho chat từ entity; thuần chuyển đổi, không truy cập dữ liệu. */
final class ChatResponseMapper {

    private ChatResponseMapper() {
    }

    /** {@code lastMessage} truyền vào sẵn (có thể null) để mapper không phải tự đọc repository. */
    static ConversationResponseCommand toConversationResponse(Conversation conversation, Message lastMessage) {
        List<ConversationResponseCommand.MemberItemResponse> members = conversation.getMembers().stream()
                .map(member -> ConversationResponseCommand.MemberItemResponse.builder()
                        .userId(member.getUserId())
                        .role(member.getRole())
                        .joinedAt(member.getJoinedAt())
                        .lastReadMessageId(member.getLastReadMessageId())
                        .leftAt(member.getLeftAt())
                        .build())
                .toList();
        return ConversationResponseCommand.builder()
                .conversationId(conversation.getConversationId())
                .type(conversation.getType())
                .name(conversation.getName())
                .avatarUrl(conversation.getAvatarUrl())
                .contextId(conversation.getContextId())
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageContent(lastMessage == null ? null : lastMessage.getContent())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastSenderId(lastMessage == null ? null : lastMessage.getSenderId())
                .members(members)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    static MessageResponseCommand toMessageResponse(Message message) {
        List<MessageResponseCommand.AttachmentResponseItem> attachments = Optional.ofNullable(message.getAttachments())
                .orElseGet(List::of).stream()
                .map(attachment -> MessageResponseCommand.AttachmentResponseItem.builder()
                        .attachmentId(attachment.getAttachmentId())
                        .storageKey(attachment.getStorageKey())
                        .type(attachment.getType())
                        .fileName(attachment.getFileName())
                        .fileSize(attachment.getFileSize())
                        .build()).toList();
        List<MessageResponseCommand.ReceiptResponseItem> receipts = Optional.ofNullable(message.getReceipts())
                .orElseGet(List::of).stream()
                .map(receipt -> MessageResponseCommand.ReceiptResponseItem.builder()
                        .receiptId(receipt.getReceiptId())
                        .userId(receipt.getUserId())
                        .status(receipt.getStatus())
                        .occurredAt(receipt.getOccurredAt())
                        .build()).toList();
        return MessageResponseCommand.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .clientMessageId(message.getClientMessageId())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .replyToMessageId(message.getReplyToMessageId())
                .attachments(attachments)
                .receipts(receipts)
                .deleted(message.getDeleted())
                .sentAt(message.getSentAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}
