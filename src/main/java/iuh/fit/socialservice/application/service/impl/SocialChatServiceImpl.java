package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageWindowCommand;
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
import iuh.fit.socialservice.domain.enums.ConversationSubjectType;
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
import java.util.Collection;
import java.util.Collections;
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
        return conversationRepository.findUserConversations(userId, pageable)
                .map(conversation -> viewerResponse(conversation, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponseCommand> getUserConversations(UUID userId, Collection<ConversationType> types,
                                                                  Pageable pageable) {
        return conversationRepository.findUserConversations(userId, types, pageable)
                .map(conversation -> viewerResponse(conversation, userId));
    }

    @Override
    @Transactional
    public ConversationResponseCommand getOrCreateBusinessConversation(
            UUID playerId, UUID ownerId, ConversationSubjectType subjectType, UUID subjectId) throws ServiceException {
        if (playerId.equals(ownerId)) {
            throw new ServiceException("Bạn không thể nhắn tin cho chính mình.");
        }
        if (subjectType == null || subjectId == null) {
            throw new ServiceException("Thiếu sân, giải đấu hoặc vé đặt sân cần hỏi.");
        }
        if (socialBlockService.isBlocked(playerId, ownerId)) {
            throw new ServiceException("Không thể tạo cuộc trò chuyện do quan hệ người dùng đã bị chặn.");
        }
        LocalDateTime now = LocalDateTime.now();
        // ponytail: mot doan chat cho moi cap nguoi, khong phan biet chieu (A hoi B hay B hoi A); tach theo chieu neu
        // co chu san vua la khach cua chu san kia ma can hai hop thu rieng.
        Optional<Conversation> existing = conversationRepository.findPairOfType(ConversationType.BUSINESS, playerId, ownerId);
        if (existing.isPresent()) {
            Conversation conversation = existing.get();
            if (subjectType != conversation.getSubjectType() || !subjectId.equals(conversation.getSubjectId())) {
                conversation.setSubjectType(subjectType);
                conversation.setSubjectId(subjectId);
                conversation.setUpdatedAt(now);
                conversation = conversationRepository.save(conversation);
            }
            return viewerResponse(conversation, playerId);
        }
        Conversation conversation = Conversation.builder()
                .type(ConversationType.BUSINESS)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .members(List.of(newMember(ownerId, MemberRole.OWNER, now), newMember(playerId, MemberRole.MEMBER, now)))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return viewerResponse(conversationRepository.save(conversation), playerId);
    }

    @Override
    @Transactional
    public ConversationResponseCommand getOrCreateSupportConversation(UUID ownerId, UUID adminId) throws ServiceException {
        if (ownerId == null) throw new ServiceException("Thiếu chủ sân.");
        if (ownerId.equals(adminId)) throw new ServiceException("Bạn không thể nhắn tin cho chính mình.");
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = conversationRepository.findSupportOf(ownerId).orElse(null);
        if (conversation == null) {
            List<ConversationMember> members = new ArrayList<>();
            members.add(newMember(ownerId, MemberRole.OWNER, now));
            if (adminId != null) members.add(newMember(adminId, MemberRole.ADMIN, now));
            conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.SUPPORT)
                    .name(SUPPORT_NAME)
                    .members(members)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            return viewerResponse(conversation, adminId != null ? adminId : ownerId);
        }
        if (adminId != null) {
            List<ConversationMember> members = new ArrayList<>(conversation.getMembers());
            Optional<ConversationMember> admin = members.stream()
                    .filter(member -> adminId.equals(member.getUserId())).findFirst();
            boolean changed = true;
            if (admin.isEmpty()) {
                members.add(newMember(adminId, MemberRole.ADMIN, now));
            } else if (admin.get().getLeftAt() != null) {
                admin.get().setLeftAt(null);
                admin.get().setJoinedAt(now);
            } else {
                changed = false;
            }
            if (changed) {
                conversation.setMembers(members);
                conversation.setUpdatedAt(now);
                conversation = conversationRepository.save(conversation);
            }
        }
        return viewerResponse(conversation, adminId != null ? adminId : ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponseCommand> getSupportInbox(UUID adminId, Pageable pageable) {
        return conversationRepository.findByType(ConversationType.SUPPORT, pageable)
                .map(conversation -> viewerResponse(conversation, adminId));
    }

    /** Ten doan chat SUPPORT phia chu san; admin thay ten chu san (frontend lay tu thanh vien OWNER). */
    static final String SUPPORT_NAME = "Hỗ trợ GOAT Sports";

    @Override
    @Transactional(readOnly = true)
    public ConversationResponseCommand getConversation(UUID conversationId, UUID userId) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        requireActiveMember(conversation, userId);
        return viewerResponse(conversation, userId);
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
        if (existing.isPresent()) return viewerResponse(existing.get(), firstUserId);

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
        ConversationMember member = requireActiveMember(conversation, userId);
        return messageRepository.findByConversationId(conversationId, member.getClearedAt(), pageable)
                .map(ChatResponseMapper::toMessageResponse);
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

        // Chat hai người (riêng hoặc kèo 1-1): một trong hai đã chặn người kia thì không gửi được nữa (hai chiều).
        long activeMembers = conversation.getMembers().stream().filter(member -> member.getLeftAt() == null).count();
        if (conversation.getType() == ConversationType.DIRECT || conversation.getType() == ConversationType.BUSINESS
                || (conversation.getType() == ConversationType.MATCH && activeMembers == 2)) {
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
        // Kenh rieng tung nguoi nhan cho bong bong chat; nguoi da tat thong bao thi khong day.
        conversation.getMembers().stream()
                .filter(member -> member.getLeftAt() == null && !member.isMuted()
                        && !member.getUserId().equals(command.getSenderId()))
                .forEach(member -> webSocketPublisher.publishMessageToUser(member.getUserId(), savedMessage));
        if (conversation.getType() == ConversationType.SUPPORT) webSocketPublisher.publishSupportMessage(savedMessage);
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

    /** Tin ngu canh hien truoc vach "Tin nhan chua doc". */
    private static final int UNREAD_CONTEXT = 10;

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseCommand> getMessagesByCursor(UUID conversationId, UUID userId, LocalDateTime before,
                                                            LocalDateTime after, int size) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        LocalDateTime floor = requireActiveMember(conversation, userId).getClearedAt();
        int limit = Math.max(1, Math.min(size, 100));
        List<Message> messages = after != null
                ? newestFirst(messageRepository.findNewer(conversationId, after, false, floor, limit))
                : messageRepository.findOlder(conversationId, before == null ? LocalDateTime.now().plusDays(1) : before,
                        floor, limit);
        return messages.stream().map(ChatResponseMapper::toMessageResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MessageWindowCommand getUnreadWindow(
            UUID conversationId, UUID userId, int size) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        LocalDateTime floor = requireActiveMember(conversation, userId).getClearedAt();
        int limit = Math.max(1, Math.min(size, 100));
        Optional<Message> firstUnread = messageRepository.findFirstUnread(conversationId, userId, floor);
        if (firstUnread.isEmpty()) {
            List<Message> latest = messageRepository.findOlder(conversationId, LocalDateTime.now().plusDays(1), floor, limit);
            return new MessageWindowCommand(
                    latest.stream().map(ChatResponseMapper::toMessageResponse).toList(), null, latest.size() == limit, false);
        }
        LocalDateTime anchor = firstUnread.get().getSentAt();
        List<Message> newer = messageRepository.findNewer(conversationId, anchor, true, floor, limit);
        List<Message> older = messageRepository.findOlder(conversationId, anchor, floor, UNREAD_CONTEXT);
        List<Message> window = new ArrayList<>(newestFirst(newer));
        window.addAll(older);
        return new MessageWindowCommand(
                window.stream().map(ChatResponseMapper::toMessageResponse).toList(),
                firstUnread.get().getMessageId(), older.size() == UNREAD_CONTEXT, newer.size() == limit);
    }

    private static List<Message> newestFirst(List<Message> oldestFirst) {
        List<Message> reversed = new ArrayList<>(oldestFirst);
        Collections.reverse(reversed);
        return reversed;
    }

    @Override
    @Transactional
    public void clearConversation(UUID conversationId, UUID userId) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        ConversationMember member = requireActiveMember(conversation, userId);
        messageRepository.markConversationAsRead(conversationId, userId);
        LocalDateTime now = LocalDateTime.now();
        member.setClearedAt(now);
        member.setLastReadMessageId(conversation.getLastMessageId());
        member.setUpdatedAt(now);
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void setMuted(UUID conversationId, UUID userId, boolean muted) throws ServiceException {
        Conversation conversation = requireConversation(conversationId);
        ConversationMember member = requireActiveMember(conversation, userId);
        member.setMuted(muted);
        member.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    /** Phan rieng cua nguoi xem: so tin chua doc, tat thong bao, quan he chan (chat hai nguoi). */
    private ConversationResponseCommand viewerResponse(Conversation conversation, UUID viewerId) {
        ConversationResponseCommand response = mapToConversationResponse(conversation);
        response.setUnreadCount(messageRepository.countUnreadMessages(conversation.getConversationId(), viewerId));
        List<ConversationMember> active = conversation.getMembers().stream()
                .filter(member -> member.getLeftAt() == null)
                .toList();
        active.stream().filter(member -> viewerId.equals(member.getUserId())).findFirst()
                .ifPresent(member -> response.setMuted(member.isMuted()));
        if (conversation.getType() == ConversationType.SUPPORT) {
            UUID ownerId = conversation.getMembers().stream().filter(member -> member.getRole() == MemberRole.OWNER)
                    .map(ConversationMember::getUserId).findFirst().orElse(null);
            response.setAwaitingReply(ownerId != null && ownerId.equals(response.getLastSenderId()));
        }
        if (active.size() == 2 && (conversation.getType() == ConversationType.DIRECT
                || conversation.getType() == ConversationType.BUSINESS
                || conversation.getType() == ConversationType.MATCH)) {
            active.stream().map(ConversationMember::getUserId).filter(id -> !id.equals(viewerId)).findFirst()
                    .ifPresent(other -> {
                        if (socialBlockService.hasBlocked(viewerId, other)) response.setBlockState("BLOCKED_BY_ME");
                        else if (socialBlockService.hasBlocked(other, viewerId)) response.setBlockState("BLOCKED_BY_THEM");
                    });
        }
        return response;
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
