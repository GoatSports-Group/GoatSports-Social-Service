package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.presentation.request.CreateConversationRequest;
import iuh.fit.socialservice.presentation.request.SendMessageRequest;
import iuh.fit.socialservice.presentation.response.ConversationResponse;
import iuh.fit.socialservice.presentation.response.MessageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SocialChatConverter {

    public CreateConversationCommand toCommand(CreateConversationRequest req) {
        List<CreateConversationCommand.MemberItem> members = null;
        if (req.getMembers() != null) {
            members = req.getMembers().stream().map(m -> CreateConversationCommand.MemberItem.builder()
                    .userId(m.getUserId())
                    .build()
            ).collect(Collectors.toList());
        }

        return CreateConversationCommand.builder()
                .type(req.getType())
                .name(req.getName())
                .avatarUrl(req.getAvatarUrl())
                .contextId(req.getContextId())
                .creatorId(req.getCreatorId())
                .members(members)
                .build();
    }

    public SendMessageCommand toCommand(UUID conversationId, SendMessageRequest req) {
        List<SendMessageCommand.AttachmentItem> attachments = req.getAttachments() == null
                ? List.of()
                : req.getAttachments().stream().map(item -> SendMessageCommand.AttachmentItem.builder()
                        .attachmentId(item.getAttachmentId())
                        .storageKey(item.getStorageKey())
                        .type(item.getType())
                        .fileName(item.getFileName())
                        .fileSize(item.getFileSize())
                        .build())
                .toList();
        return SendMessageCommand.builder()
                .conversationId(conversationId)
                .senderId(req.getSenderId())
                .clientMessageId(req.getClientMessageId())
                .content(req.getContent())
                .type(req.getType())
                .replyToMessageId(req.getReplyToMessageId())
                .attachments(attachments)
                .build();
    }

    public ConversationResponse toResponse(ConversationResponseCommand cmd) {
        List<ConversationResponse.MemberItemResponse> members = null;
        if (cmd.getMembers() != null) {
            members = cmd.getMembers().stream().map(m -> ConversationResponse.MemberItemResponse.builder()
                    .userId(m.getUserId())
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .lastReadMessageId(m.getLastReadMessageId())
                    .leftAt(m.getLeftAt())
                    .build()
            ).collect(Collectors.toList());
        }

        return ConversationResponse.builder()
                .conversationId(cmd.getConversationId())
                .type(cmd.getType())
                .name(cmd.getName())
                .avatarUrl(cmd.getAvatarUrl())
                .contextId(cmd.getContextId())
                .lastMessageId(cmd.getLastMessageId())
                .lastMessageContent(cmd.getLastMessageContent())
                .lastMessageAt(cmd.getLastMessageAt())
                .lastSenderId(cmd.getLastSenderId())
                .unreadCount(cmd.getUnreadCount())
                .muted(cmd.getMuted())
                .blockState(cmd.getBlockState())
                .members(members)
                .createdAt(cmd.getCreatedAt())
                .updatedAt(cmd.getUpdatedAt())
                .build();
    }

    public MessageResponse toResponse(MessageResponseCommand cmd) {
        List<MessageResponse.AttachmentResponseItem> attachments = null;
        if (cmd.getAttachments() != null) {
            attachments = cmd.getAttachments().stream().map(a -> MessageResponse.AttachmentResponseItem.builder()
                    .attachmentId(a.getAttachmentId())
                    .storageKey(a.getStorageKey())
                    .fileName(a.getFileName())
                    .fileSize(a.getFileSize())
                    .type(a.getType())
                    .build()
            ).collect(Collectors.toList());
        }

        List<MessageResponse.ReceiptResponseItem> receipts = null;
        if (cmd.getReceipts() != null) {
            receipts = cmd.getReceipts().stream().map(receipt -> MessageResponse.ReceiptResponseItem.builder()
                    .receiptId(receipt.getReceiptId())
                    .userId(receipt.getUserId())
                    .status(receipt.getStatus())
                    .occurredAt(receipt.getOccurredAt())
                    .build()).toList();
        }

        return MessageResponse.builder()
                .messageId(cmd.getMessageId())
                .conversationId(cmd.getConversationId())
                .senderId(cmd.getSenderId())
                .clientMessageId(cmd.getClientMessageId())
                .content(cmd.getContent())
                .type(cmd.getType())
                .status(cmd.getStatus())
                .replyToMessageId(cmd.getReplyToMessageId())
                .attachments(attachments)
                .deleted(cmd.getDeleted())
                .sentAt(cmd.getSentAt())
                .editedAt(cmd.getEditedAt())
                .receipts(receipts)
                .build();
    }
}
