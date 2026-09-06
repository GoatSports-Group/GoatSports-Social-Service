package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.domain.entity.MessageAttachment;
import iuh.fit.socialservice.domain.entity.MessageReceipt;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.domain.enums.MemberRole;
import iuh.fit.socialservice.domain.enums.MessageStatus;
import iuh.fit.socialservice.domain.enums.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialChatServiceImpl implements SocialChatService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SocialBlockService socialBlockService;
    private final SocialWebSocketPublisher webSocketPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponseCommand> getUserConversations(UUID userId, Pageable pageable) {
        return conversationRepository.findUserConversations(userId, pageable).map(conversation -> {
            ConversationResponseCommand response = mapToConversationResponse(conversation);
            response.setUnreadCount(messageRepository.countUnreadMessages(conversation.getConversationId(), userId));
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponseCommand getConversation(UUID conversationId, UUID userId) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        requireActiveMember(conversation, userId);
        ConversationResponseCommand response = mapToConversationResponse(conversation);
        response.setUnreadCount(messageRepository.countUnreadMessages(conversationId, userId));
        return response;
    }

    @Override
    @Transactional
    public ConversationResponseCommand getOrCreateDirectConversation(
            UUID firstUserId,
            UUID secondUserId
    ) throws ServiceException {
        if (firstUserId.equals(secondUserId)) {
            throw new ServiceException("Không thể tạo cuộc trò chuyện với chính mình.");
        }
        if (socialBlockService.isBlocked(firstUserId, secondUserId)) {
            throw new ServiceException("Không thể tạo cuộc trò chuyện do quan hệ người dùng đã bị chặn.");
        }

        Optional<Conversation> existing = conversationRepository.findDirectBetween(firstUserId, secondUserId);
        if (existing.isPresent()) return mapToConversationResponse(existing.get());

        LocalDateTime now = LocalDateTime.now();
        List<ConversationMember> members = List.of(
                newMember(firstUserId, MemberRole.MEMBER, now),
                newMember(secondUserId, MemberRole.MEMBER, now)
        );
        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .members(members)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return mapToConversationResponse(conversationRepository.save(conversation));
    }

    @Override
    @Transactional
    public ConversationResponseCommand createGroupConversation(CreateConversationCommand command) throws ServiceException {
        if (command.getCreatorId() == null) throw new ServiceException("Thiếu mã người tạo nhóm.");
        if (command.getName() == null || command.getName().isBlank()) {
            throw new ServiceException("Tên nhóm trò chuyện không được để trống.");
        }

        Map<UUID, CreateConversationCommand.MemberItem> distinctMembers = new LinkedHashMap<>();
        if (command.getMembers() != null) {
            command.getMembers().stream()
                    .filter(item -> item.getUserId() != null)
                    .forEach(item -> distinctMembers.putIfAbsent(item.getUserId(), item));
        }
        distinctMembers.putIfAbsent(command.getCreatorId(), CreateConversationCommand.MemberItem.builder()
                .userId(command.getCreatorId())
                .build());
        if (distinctMembers.size() < 3) {
            throw new ServiceException("Nhóm trò chuyện cần ít nhất 3 thành viên.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<ConversationMember> members = distinctMembers.keySet().stream()
                .map(userId -> newMember(
                        userId,
                        userId.equals(command.getCreatorId()) ? MemberRole.OWNER : MemberRole.MEMBER,
                        now
                ))
                .toList();
        Conversation conversation = Conversation.builder()
                .type(command.getType() == null ? ConversationType.GROUP : command.getType())
                .name(command.getName().trim())
                .avatarUrl(command.getAvatarUrl())
                .contextId(command.getContextId())
                .members(members)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return mapToConversationResponse(conversationRepository.save(conversation));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponseCommand> getConversationMessages(
            UUID conversationId,
            UUID userId,
            Pageable pageable
    ) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        requireActiveMember(conversation, userId);
        return messageRepository.findByConversationId(conversationId, pageable).map(this::mapToMessageResponse);
    }

    @Override
    @Transactional
    public MessageResponseCommand sendMessage(SendMessageCommand command) throws ServiceException {
        Conversation conversation = requireConversation(command.getConversationId());
        requireActiveMember(conversation, command.getSenderId());
        validateMessage(command);

        if (conversation.getType() == ConversationType.DIRECT) {
            for (ConversationMember member : conversation.getMembers()) {
                if (member.getLeftAt() == null && !member.getUserId().equals(command.getSenderId()) &&
                        socialBlockService.isBlocked(command.getSenderId(), member.getUserId())) {
                    throw new ServiceException("Không thể gửi tin nhắn do quan hệ người dùng đã bị chặn.");
                }
            }
        }

        String clientMessageId = command.getClientMessageId() == null || command.getClientMessageId().isBlank()
                ? UUID.randomUUID().toString()
                : command.getClientMessageId().trim();
        Optional<Message> duplicate = messageRepository.findBySenderIdAndClientMessageId(
                command.getSenderId(),
                clientMessageId
        );
        if (duplicate.isPresent()) return mapToMessageResponse(duplicate.get());

        if (command.getReplyToMessageId() != null) {
            Message repliedMessage = messageRepository.findById(command.getReplyToMessageId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy tin nhắn được phản hồi."));
            if (!command.getConversationId().equals(repliedMessage.getConversationId())) {
                throw new ServiceException("Tin nhắn phản hồi không thuộc cuộc trò chuyện này.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<MessageReceipt> receipts = conversation.getMembers().stream()
                .filter(member -> member.getLeftAt() == null && !member.getUserId().equals(command.getSenderId()))
                .map(member -> MessageReceipt.builder()
                        .receiptId(UUID.randomUUID())
                        .userId(member.getUserId())
                        .status(MessageStatus.SENT)
                        .occurredAt(now)
                        .build())
                .toList();
        List<MessageAttachment> attachments = command.getAttachments() == null
                ? List.of()
                : command.getAttachments().stream().map(item -> MessageAttachment.builder()
                        .attachmentId(item.getAttachmentId() == null ? UUID.randomUUID() : item.getAttachmentId())
                        .storageKey(item.getStorageKey())
                        .type(item.getType())
                        .fileName(item.getFileName())
                        .fileSize(item.getFileSize())
                        .build()).toList();

        Message message = Message.builder()
                .conversationId(command.getConversationId())
                .senderId(command.getSenderId())
                .clientMessageId(clientMessageId)
                .content(command.getContent() == null ? "" : command.getContent().trim())
                .type(command.getType() == null ? MessageType.TEXT : command.getType())
                .status(MessageStatus.SENT)
                .replyToMessageId(command.getReplyToMessageId())
                .deleted(false)
                .sentAt(now)
                .attachments(attachments)
                .receipts(receipts)
                .build();
        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessageId(savedMessage.getMessageId());
        conversation.setLastMessageAt(savedMessage.getSentAt());
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
        webSocketPublisher.publishMessageToConversation(command.getConversationId(), savedMessage);
        return mapToMessageResponse(savedMessage);
    }

    @Override
    @Transactional
    public void markAsRead(UUID conversationId, UUID userId) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        ConversationMember member = requireActiveMember(conversation, userId);
        messageRepository.markConversationAsRead(conversationId, userId);
        member.setLastReadMessageId(conversation.getLastMessageId());
        member.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private Conversation requireConversation(UUID conversationId) throws ServiceException {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy cuộc trò chuyện."));
    }

    private ConversationMember requireActiveMember(Conversation conversation, UUID userId) throws ServiceException {
        if (userId == null) throw new ServiceException("Thiếu mã người dùng.");
        return conversation.getMembers().stream()
                .filter(member -> userId.equals(member.getUserId()) && member.getLeftAt() == null)
                .findFirst()
                .orElseThrow(() -> new ServiceException("Bạn không phải thành viên của cuộc trò chuyện này."));
    }

    private void validateMessage(SendMessageCommand command) throws ServiceException {
        boolean hasContent = command.getContent() != null && !command.getContent().trim().isEmpty();
        boolean hasAttachments = command.getAttachments() != null && !command.getAttachments().isEmpty();
        if (!hasContent && !hasAttachments) {
            throw new ServiceException("Tin nhắn phải có nội dung hoặc tệp đính kèm.");
        }
        if (hasAttachments && command.getAttachments().stream().anyMatch(item ->
                item.getStorageKey() == null || item.getStorageKey().isBlank() || item.getType() == null)) {
            throw new ServiceException("Thông tin tệp đính kèm không hợp lệ.");
        }
    }

    private ConversationMember newMember(UUID userId, MemberRole role, LocalDateTime joinedAt) {
        return ConversationMember.builder()
                .memberId(UUID.randomUUID())
                .userId(userId)
                .role(role)
                .joinedAt(joinedAt)
                .createdAt(joinedAt)
                .updatedAt(joinedAt)
                .build();
    }

    private ConversationResponseCommand mapToConversationResponse(Conversation conversation) {
        Message lastMessage = conversation.getLastMessageId() == null
                ? null
                : messageRepository.findById(conversation.getLastMessageId()).orElse(null);
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

    private MessageResponseCommand mapToMessageResponse(Message message) {
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
