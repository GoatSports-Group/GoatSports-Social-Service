package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
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
import iuh.fit.socialservice.application.port.external.StorageKeyPromoter;
import iuh.fit.socialservice.domain.entity.MessageAttachment;
import iuh.fit.socialservice.domain.service.ChatAttachmentPolicy;
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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
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
    private final StorageKeyPromoter storageKeyPromoter;

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
        Map<UUID, CreateConversationCommand.MemberItem> distinctMembers = new LinkedHashMap<>();
        if (command.getMembers() != null) {
            command.getMembers().stream()
                    .filter(item -> item.getUserId() != null)
                    .forEach(item -> distinctMembers.putIfAbsent(item.getUserId(), item));
        }
        distinctMembers.putIfAbsent(command.getCreatorId(), CreateConversationCommand.MemberItem.builder()
                .userId(command.getCreatorId())
                .build());
        command.validate(distinctMembers.size());

        Optional<Conversation> reusable = findReusableConversation(command, List.copyOf(distinctMembers.keySet()));
        if (reusable.isPresent()) {
            return mapToConversationResponse(bindContext(reusable.get(), command));
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
    @Transactional
    public ConversationResponseCommand syncContextConversation(SyncContextConversationCommand command)
            throws ServiceException {
        command.validate();
        LocalDateTime now = LocalDateTime.now();
        Set<UUID> desired = new LinkedHashSet<>();
        desired.add(command.getOwnerId());
        if (command.getMemberIds() != null) {
            command.getMemberIds().stream().filter(Objects::nonNull).forEach(desired::add);
        }

        Conversation conversation = conversationRepository.findByContextId(command.getContextId()).orElse(null);
        if (conversation == null) {
            List<ConversationMember> members = desired.stream()
                    .map(userId -> newMember(userId,
                            userId.equals(command.getOwnerId()) ? MemberRole.OWNER : MemberRole.MEMBER, now))
                    .toList();
            conversation = Conversation.builder()
                    .type(command.getType() == null ? ConversationType.CLUB : command.getType())
                    .name(command.getName().trim())
                    .avatarUrl(command.getAvatarUrl())
                    .contextId(command.getContextId())
                    .members(members)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            return mapToConversationResponse(conversationRepository.save(conversation));
        }

        conversation.setName(command.getName().trim());
        if (command.getAvatarUrl() != null) conversation.setAvatarUrl(command.getAvatarUrl());

        List<ConversationMember> members = conversation.getMembers() == null
                ? new ArrayList<>() : new ArrayList<>(conversation.getMembers());
        Set<UUID> present = new LinkedHashSet<>();
        for (ConversationMember member : members) {
            boolean shouldBeActive = desired.contains(member.getUserId());
            if (shouldBeActive) {
                present.add(member.getUserId());
                if (member.getLeftAt() != null) {
                    member.setLeftAt(null);
                    member.setJoinedAt(now);
                }
            } else if (member.getLeftAt() == null) {
                member.setLeftAt(now);
            }
        }
        desired.stream().filter(userId -> !present.contains(userId))
                .forEach(userId -> members.add(newMember(userId,
                        userId.equals(command.getOwnerId()) ? MemberRole.OWNER : MemberRole.MEMBER, now)));

        conversation.setMembers(members);
        conversation.setUpdatedAt(now);
        return mapToConversationResponse(conversationRepository.save(conversation));
    }

    private Optional<Conversation> findReusableConversation(CreateConversationCommand command, List<UUID> memberIds) {
        Optional<Conversation> sameContext = conversationRepository.findByContextId(command.getContextId());
        if (sameContext.isPresent()) return sameContext;
        if (command.getType() != ConversationType.MATCH || memberIds.size() != 2) return Optional.empty();
        return conversationRepository.findPairConversation(memberIds.get(0), memberIds.get(1));
    }

    /**
     * Gắn kèo hiện tại vào luồng chat đang dùng lại. Luồng chat cá nhân (DIRECT) giữ
     * nguyên tên và kiểu để không bị đổi thành "Kèo ..." trước mặt người dùng.
     */
    private Conversation bindContext(Conversation conversation, CreateConversationCommand command) {
        boolean changed = false;
        if (command.getContextId() != null && !command.getContextId().equals(conversation.getContextId())) {
            conversation.setContextId(command.getContextId());
            changed = true;
        }
        if (conversation.getType() == ConversationType.MATCH
                && command.getName() != null
                && !command.getName().trim().equals(conversation.getName())) {
            conversation.setName(command.getName().trim());
            changed = true;
        }
        if (!changed) return conversation;
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
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
        return messageRepository.findByConversationId(conversationId, pageable).map(ChatResponseMapper::toMessageResponse);
    }

    @Override
    @Transactional
    public MessageResponseCommand sendMessage(SendMessageCommand command) throws ServiceException {
        Conversation conversation = requireConversation(command.getConversationId());
        requireActiveMember(conversation, command.getSenderId());
        command.validate();
        String attachmentProblem = ChatAttachmentPolicy.violation(
                command.getAttachments() == null ? List.of() : command.getAttachments().stream()
                        .map(item -> item == null ? null
                                : new ChatAttachmentPolicy.Item(item.getStorageKey(), item.getType(), item.getFileSize()))
                        .toList(),
                command.getSenderId());
        if (attachmentProblem != null) throw new ServiceException(attachmentProblem);

        // Chat 1-1: một trong hai người đã chặn người kia thì không gửi được nữa (chặn có hiệu lực hai chiều).
        if (conversation.getType() == ConversationType.DIRECT) {
            for (ConversationMember member : conversation.getMembers()) {
                if (member.getLeftAt() == null && !member.getUserId().equals(command.getSenderId()) &&
                        socialBlockService.isBlocked(command.getSenderId(), member.getUserId())) {
                    throw new ServiceException("Không thể gửi tin nhắn do quan hệ người dùng đã bị chặn.");
                }
            }
        }

        // Chống gửi trùng khi client gửi lại (mất mạng, reconnect WebSocket): cùng clientMessageId của cùng người gửi
        // thì trả lại tin đã lưu. Có unique index (senderId, clientMessageId) ở MongoDB làm chốt chặn cuối.
        String clientMessageId = command.getClientMessageId() == null || command.getClientMessageId().isBlank()
                ? UUID.randomUUID().toString()
                : command.getClientMessageId().trim();
        Optional<Message> duplicate = messageRepository.findBySenderIdAndClientMessageId(
                command.getSenderId(),
                clientMessageId
        );
        if (duplicate.isPresent()) return ChatResponseMapper.toMessageResponse(duplicate.get());

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
        List<MessageAttachment> attachments = toAttachments(command.getAttachments(), command.getConversationId());

        Message message = Message.builder()
                .conversationId(command.getConversationId())
                .senderId(command.getSenderId())
                .clientMessageId(clientMessageId)
                .content(command.getContent() == null ? "" : command.getContent().trim())
                .type(command.getType() != null ? command.getType()
                        : attachments.isEmpty() ? MessageType.TEXT : MessageType.IMAGE)
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
        return ChatResponseMapper.toMessageResponse(savedMessage);
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

    /**
     * Ca nhom anh di trong mot tin nhan, giu dung thu tu nguoi gui chon. Anh vua tai len nam o R2 `temp/`
     * (lifecycle rule se xoa); doi sang vung vinh vien truoc khi luu khoa, giong anh dai dien va bai viet.
     */
    private List<MessageAttachment> toAttachments(List<SendMessageCommand.AttachmentItem> items, UUID conversationId) {
        if (items == null || items.isEmpty()) return List.of();
        List<String> keys = storageKeyPromoter.promote(
                items.stream().map(item -> item.getStorageKey().trim()).toList(),
                "chat-message:" + conversationId);
        List<MessageAttachment> attachments = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            SendMessageCommand.AttachmentItem item = items.get(index);
            attachments.add(MessageAttachment.builder()
                    .attachmentId(UUID.randomUUID())
                    .storageKey(keys.get(index))
                    .type(item.getType())
                    .fileName(item.getFileName())
                    .fileSize(item.getFileSize())
                    .build());
        }
        return attachments;
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

    private ConversationMember newMember(UUID userId, MemberRole role, LocalDateTime joinedAt) {
        return ConversationMember.builder()
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
        return ChatResponseMapper.toConversationResponse(conversation, lastMessage);
    }
}
