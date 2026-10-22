package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageWindowCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.external.StorageKeyPromoter;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.domain.entity.MessageAttachment;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.ConversationSubjectType;
import iuh.fit.socialservice.domain.enums.MemberRole;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.domain.enums.MessageType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialChatServiceImplTest {
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SocialBlockService socialBlockService;
    @Mock
    private SocialWebSocketPublisher webSocketPublisher;
    @Mock
    private StorageKeyPromoter storageKeyPromoter;

    private SocialChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocialChatServiceImpl(
                conversationRepository,
                messageRepository,
                socialBlockService,
                webSocketPublisher,
                storageKeyPromoter
        );
        Mockito.lenient().when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createGroupConversationLeavesGeneratedMemberIdsUnset() throws ServiceException {
        UUID creatorId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();

        service.createGroupConversation(CreateConversationCommand.builder()
                .type(ConversationType.MATCH)
                .name("Kèo cầu lông")
                .creatorId(creatorId)
                .members(List.of(
                        CreateConversationCommand.MemberItem.builder().userId(creatorId).build(),
                        CreateConversationCommand.MemberItem.builder().userId(opponentId).build()
                ))
                .build());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        Mockito.verify(conversationRepository).save(captor.capture());
        Conversation conversation = captor.getValue();

        assertThat(conversation.getMembers()).hasSize(2);
        assertThat(conversation.getMembers())
                .allSatisfy(member -> assertThat(member.getMemberId()).isNull());
    }

    @Test
    void matchConversationReusesTheExistingThreadBetweenTheSameTwoPlayers() throws ServiceException {
        UUID creatorId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        UUID newSessionId = UUID.randomUUID();
        Conversation existing = Conversation.builder()
                .conversationId(existingId)
                .type(ConversationType.DIRECT)
                .name("Thắng Đạt")
                .contextId(UUID.randomUUID())
                .members(List.of())
                .build();
        when(conversationRepository.findPairConversation(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(existing));

        ConversationResponseCommand response = service.createGroupConversation(CreateConversationCommand.builder()
                .type(ConversationType.MATCH)
                .name("Kèo BADMINTON")
                .contextId(newSessionId)
                .creatorId(creatorId)
                .members(List.of(
                        CreateConversationCommand.MemberItem.builder().userId(creatorId).build(),
                        CreateConversationCommand.MemberItem.builder().userId(opponentId).build()
                ))
                .build());

        assertThat(response.getConversationId()).isEqualTo(existingId);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        Mockito.verify(conversationRepository).save(captor.capture());
        Conversation saved = captor.getValue();
        assertThat(saved.getConversationId()).isEqualTo(existingId);
        assertThat(saved.getContextId()).isEqualTo(newSessionId);
        assertThat(saved.getName()).isEqualTo("Thắng Đạt");
        assertThat(saved.getType()).isEqualTo(ConversationType.DIRECT);
    }

    @Test
    void matchConversationIsNotRecreatedForASessionThatAlreadyHasOne() throws ServiceException {
        UUID creatorId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        when(conversationRepository.findByContextId(sessionId)).thenReturn(Optional.of(
                Conversation.builder()
                        .conversationId(existingId)
                        .type(ConversationType.MATCH)
                        .name("Kèo BADMINTON")
                        .contextId(sessionId)
                        .members(List.of())
                        .build()
        ));

        ConversationResponseCommand response = service.createGroupConversation(CreateConversationCommand.builder()
                .type(ConversationType.MATCH)
                .name("Kèo BADMINTON")
                .contextId(sessionId)
                .creatorId(creatorId)
                .members(List.of(
                        CreateConversationCommand.MemberItem.builder().userId(creatorId).build(),
                        CreateConversationCommand.MemberItem.builder().userId(opponentId).build()
                ))
                .build());

        assertThat(response.getConversationId()).isEqualTo(existingId);
        Mockito.verify(conversationRepository, Mockito.never()).save(any(Conversation.class));
    }

    @Test
    void createDirectConversationLeavesGeneratedMemberIdsUnset() throws ServiceException {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        service.getOrCreateDirectConversation(firstUserId, secondUserId);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        Mockito.verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getMembers())
                .allSatisfy(member -> assertThat(member.getMemberId()).isNull());
    }

    private Conversation clubConversation(UUID contextId, UUID ownerId, List<UUID> memberIds) {
        LocalDateTime now = LocalDateTime.now();
        List<ConversationMember> members = new ArrayList<>();
        members.add(ConversationMember.builder().memberId(UUID.randomUUID()).userId(ownerId)
                .role(MemberRole.OWNER).joinedAt(now).build());
        memberIds.forEach(userId -> members.add(ConversationMember.builder()
                .memberId(UUID.randomUUID()).userId(userId).role(MemberRole.MEMBER).joinedAt(now).build()));
        return Conversation.builder()
                .conversationId(UUID.randomUUID()).type(ConversationType.CLUB).name("CLB test")
                .contextId(contextId).members(members).createdAt(now).updatedAt(now).build();
    }

    @Test
    @DisplayName("chưa có chat nhóm thì tạo mới kèm chủ câu lạc bộ")
    void syncCreatesConversationWhenMissing() throws ServiceException {
        UUID clubId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(conversationRepository.findByContextId(clubId)).thenReturn(Optional.empty());

        service.syncContextConversation(SyncContextConversationCommand.builder()
                .contextId(clubId).type(ConversationType.CLUB).name("CLB Sài Gòn")
                .ownerId(ownerId).memberIds(List.of(memberId)).build());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        Mockito.verify(conversationRepository).save(captor.capture());
        Conversation saved = captor.getValue();
        assertThat(saved.getContextId()).isEqualTo(clubId);
        assertThat(saved.getType()).isEqualTo(ConversationType.CLUB);
        assertThat(saved.getMembers()).extracting(ConversationMember::getUserId)
                .containsExactlyInAnyOrder(ownerId, memberId);
        assertThat(saved.getMembers()).filteredOn(item -> item.getUserId().equals(ownerId))
                .allSatisfy(item -> assertThat(item.getRole()).isEqualTo(MemberRole.OWNER));
    }

    @Test
    @DisplayName("gọi lại với cùng danh sách thì không nhân đôi thành viên")
    void syncIsIdempotent() throws ServiceException {
        UUID clubId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Conversation existing = clubConversation(clubId, ownerId, List.of(memberId));
        when(conversationRepository.findByContextId(clubId)).thenReturn(Optional.of(existing));

        service.syncContextConversation(SyncContextConversationCommand.builder()
                .contextId(clubId).type(ConversationType.CLUB).name("CLB test")
                .ownerId(ownerId).memberIds(List.of(memberId)).build());

        assertThat(existing.getMembers()).hasSize(2);
        assertThat(existing.getMembers()).allSatisfy(item -> assertThat(item.getLeftAt()).isNull());
    }

    @Test
    @DisplayName("người rời câu lạc bộ được đánh dấu đã rời chứ không bị xóa khỏi lịch sử")
    void syncMarksRemovedMemberAsLeft() throws ServiceException {
        UUID clubId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID leaving = UUID.randomUUID();
        Conversation existing = clubConversation(clubId, ownerId, List.of(leaving));
        when(conversationRepository.findByContextId(clubId)).thenReturn(Optional.of(existing));

        service.syncContextConversation(SyncContextConversationCommand.builder()
                .contextId(clubId).type(ConversationType.CLUB).name("CLB test")
                .ownerId(ownerId).memberIds(List.of()).build());

        assertThat(existing.getMembers()).hasSize(2);
        assertThat(existing.getMembers()).filteredOn(item -> item.getUserId().equals(leaving))
                .allSatisfy(item -> assertThat(item.getLeftAt()).isNotNull());
    }

    @Test
    @DisplayName("thành viên quay lại được mở khóa thay vì thêm bản ghi mới")
    void syncRejoinsPreviousMember() throws ServiceException {
        UUID clubId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID returning = UUID.randomUUID();
        Conversation existing = clubConversation(clubId, ownerId, List.of(returning));
        existing.getMembers().stream()
                .filter(item -> item.getUserId().equals(returning))
                .forEach(item -> item.setLeftAt(LocalDateTime.now().minusDays(1)));
        when(conversationRepository.findByContextId(clubId)).thenReturn(Optional.of(existing));

        service.syncContextConversation(SyncContextConversationCommand.builder()
                .contextId(clubId).type(ConversationType.CLUB).name("CLB test")
                .ownerId(ownerId).memberIds(List.of(returning)).build());

        assertThat(existing.getMembers()).hasSize(2);
        assertThat(existing.getMembers()).filteredOn(item -> item.getUserId().equals(returning))
                .allSatisfy(item -> assertThat(item.getLeftAt()).isNull());
    }

    @Test
    @DisplayName("thiếu chủ sở hữu thì từ chối đồng bộ")
    void syncRequiresOwner() {
        Assertions.assertThatThrownBy(() ->
                service.syncContextConversation(SyncContextConversationCommand.builder()
                        .contextId(UUID.randomUUID()).name("CLB test").build()))
                .isInstanceOf(ServiceException.class);
    }

    // ---- images ----------------------------------------------------------------------------------

    private Conversation directWith(UUID sender, UUID other) {
        Conversation conversation = Conversation.builder()
                .conversationId(UUID.randomUUID())
                .type(ConversationType.DIRECT)
                .members(new ArrayList<>(List.of(
                        ConversationMember.builder().userId(sender).role(MemberRole.MEMBER).build(),
                        ConversationMember.builder().userId(other).role(MemberRole.MEMBER).build())))
                .build();
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));
        return conversation;
    }

    private static SendMessageCommand.AttachmentItem image(String key) {
        return SendMessageCommand.AttachmentItem.builder()
                .storageKey(key).type(AttachmentType.IMAGE).fileSize(2048L).build();
    }

    @Test
    @DisplayName("nhiều ảnh đi trong một tin nhắn, được đưa ra khỏi temp/ và giữ đúng thứ tự")
    void imagesTravelAsOneMessageAndArePromotedInOrder() throws ServiceException {
        UUID sender = UUID.randomUUID();
        Conversation conversation = directWith(sender, UUID.randomUUID());
        List<String> temp = List.of(
                "temp/chat-messages/" + sender + "/a-1.jpg",
                "temp/chat-messages/" + sender + "/b-2.jpg",
                "temp/chat-messages/" + sender + "/c-3.jpg");
        when(storageKeyPromoter.promote(eq(temp), anyString()))
                .thenReturn(temp.stream().map(key -> key.substring("temp/".length())).toList());
        when(messageRepository.findBySenderIdAndClientMessageId(any(), anyString())).thenReturn(Optional.empty());
        when(messageRepository.save(any())).thenAnswer(call -> {
            Message saved = call.getArgument(0);
            saved.setMessageId(UUID.randomUUID());
            return saved;
        });

        MessageResponseCommand response = service.sendMessage(SendMessageCommand.builder()
                .conversationId(conversation.getConversationId())
                .senderId(sender)
                .clientMessageId("c-1")
                .attachments(temp.stream().map(SocialChatServiceImplTest::image).toList())
                .build());

        ArgumentCaptor<Message> saved =
                ArgumentCaptor.forClass(Message.class);
        Mockito.verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().getType())
                .isEqualTo(MessageType.IMAGE);
        assertThat(saved.getValue().getAttachments())
                .extracting(MessageAttachment::getStorageKey)
                .containsExactly("chat-messages/" + sender + "/a-1.jpg", "chat-messages/" + sender + "/b-2.jpg",
                        "chat-messages/" + sender + "/c-3.jpg");
        assertThat(response.getAttachments()).hasSize(3);
    }

    @Test
    @DisplayName("không đính kèm được ảnh do người khác tải lên")
    void someoneElsesUploadIsRejected() {
        UUID sender = UUID.randomUUID();
        Conversation conversation = directWith(sender, UUID.randomUUID());

        Assertions.assertThatThrownBy(() -> service.sendMessage(
                        SendMessageCommand.builder()
                                .conversationId(conversation.getConversationId())
                                .senderId(sender)
                                .attachments(List.of(image("chat-messages/" + UUID.randomUUID() + "/x.jpg")))
                                .build()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("không thuộc về bạn");
        Mockito.verifyNoInteractions(storageKeyPromoter);
        Mockito.verify(messageRepository, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("xóa đoạn chat chỉ ẩn lịch sử phía người xóa, người kia vẫn đọc đủ")
    void clearingHidesHistoryOnlyForTheOneWhoCleared() throws ServiceException {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Conversation conversation = directWith(me, other);
        Pageable page = PageRequest.of(0, 20);
        when(messageRepository.findByConversationId(any(), any(), any()))
                .thenReturn(Page.empty());

        service.clearConversation(conversation.getConversationId(), me);
        service.getConversationMessages(conversation.getConversationId(), me, page);
        service.getConversationMessages(conversation.getConversationId(), other, page);

        LocalDateTime cleared = conversation.getMembers().get(0).getClearedAt();
        assertThat(cleared).isNotNull();
        assertThat(conversation.getMembers().get(1).getClearedAt()).isNull();
        Mockito.verify(messageRepository).markConversationAsRead(conversation.getConversationId(), me);
        Mockito.verify(messageRepository).findByConversationId(conversation.getConversationId(), cleared, page);
        Mockito.verify(messageRepository).findByConversationId(conversation.getConversationId(), null, page);
    }

    @Test
    @DisplayName("tin mới đẩy vào kênh riêng của người nhận, trừ người gửi và người đã tắt thông báo")
    void newMessagesReachEachRecipientsInboxExceptMutedOnes() throws ServiceException {
        UUID sender = UUID.randomUUID();
        UUID listening = UUID.randomUUID();
        UUID muted = UUID.randomUUID();
        Conversation conversation = Conversation.builder()
                .conversationId(UUID.randomUUID())
                .type(ConversationType.GROUP)
                .members(new ArrayList<>(List.of(
                        ConversationMember.builder().userId(sender).role(MemberRole.OWNER).build(),
                        ConversationMember.builder().userId(listening).role(MemberRole.MEMBER).build(),
                        ConversationMember.builder().userId(muted).role(MemberRole.MEMBER).build())))
                .build();
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));
        service.setMuted(conversation.getConversationId(), muted, true);
        when(messageRepository.findBySenderIdAndClientMessageId(any(), anyString())).thenReturn(Optional.empty());
        when(messageRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.sendMessage(SendMessageCommand.builder()
                .conversationId(conversation.getConversationId()).senderId(sender).clientMessageId("m-1")
                .content("Tối nay đá không?").build());

        Mockito.verify(webSocketPublisher).publishMessageToUser(eq(listening), any());
        Mockito.verify(webSocketPublisher, Mockito.never()).publishMessageToUser(eq(muted), any());
        Mockito.verify(webSocketPublisher, Mockito.never()).publishMessageToUser(eq(sender), any());
    }

    @Test
    @DisplayName("chat hai người cho biết ai đã chặn ai, và trạng thái tắt thông báo của người xem")
    void pairConversationTellsTheViewerWhoBlockedWhom() throws ServiceException {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Conversation conversation = directWith(me, other);
        when(socialBlockService.hasBlocked(me, other)).thenReturn(true);
        Mockito.lenient().when(socialBlockService.hasBlocked(other, me)).thenReturn(false);

        ConversationResponseCommand mine = service.getConversation(conversation.getConversationId(), me);
        assertThat(mine.getBlockState()).isEqualTo("BLOCKED_BY_ME");
        assertThat(mine.getMuted()).isFalse();

        when(socialBlockService.hasBlocked(other, me)).thenReturn(false);
        Mockito.lenient().when(socialBlockService.hasBlocked(me, other)).thenReturn(true);
        when(socialBlockService.hasBlocked(other, me)).thenReturn(false);
        ConversationResponseCommand theirs = service.getConversation(conversation.getConversationId(), other);
        assertThat(theirs.getBlockState()).isEqualTo("BLOCKED_BY_THEM");
    }

    @Test
    @DisplayName("mở đoạn chat tại tin chưa đọc đầu tiên: vài tin cũ làm ngữ cảnh, rồi từ tin đó trở đi")
    void unreadWindowStartsAtTheFirstUnreadMessage() throws ServiceException {
        UUID me = UUID.randomUUID();
        Conversation conversation = directWith(me, UUID.randomUUID());
        LocalDateTime anchor = LocalDateTime.now().minusMinutes(5);
        Message first = Message.builder()
                .messageId(UUID.randomUUID()).content("chưa đọc").sentAt(anchor).build();
        Message later = Message.builder()
                .messageId(UUID.randomUUID()).content("mới hơn").sentAt(anchor.plusMinutes(1)).build();
        Message earlier = Message.builder()
                .messageId(UUID.randomUUID()).content("đã đọc").sentAt(anchor.minusMinutes(1)).build();
        when(messageRepository.findFirstUnread(conversation.getConversationId(), me, null)).thenReturn(Optional.of(first));
        when(messageRepository.findNewer(conversation.getConversationId(), anchor, true, null, 2))
                .thenReturn(List.of(first, later));
        when(messageRepository.findOlder(conversation.getConversationId(), anchor, null, 10)).thenReturn(List.of(earlier));

        MessageWindowCommand window = service.getUnreadWindow(conversation.getConversationId(), me, 2);

        assertThat(window.messages()).extracting(item -> item.getContent()).containsExactly("mới hơn", "chưa đọc", "đã đọc");
        assertThat(window.firstUnreadMessageId()).isEqualTo(first.getMessageId());
        assertThat(window.hasNewer()).isTrue();
        assertThat(window.hasOlder()).isFalse();
    }

    @Test
    @DisplayName("chat công việc: lần đầu tạo BUSINESS (chủ sân là OWNER), lần sau dùng lại và đổi sân / giải đang hỏi")
    void businessConversationIsReusedPerPairAndFollowsTheLatestSubject() throws ServiceException {
        UUID player = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID venue = UUID.randomUUID();
        UUID tournament = UUID.randomUUID();
        when(conversationRepository.findPairOfType(ConversationType.BUSINESS, player, owner)).thenReturn(Optional.empty());

        ConversationResponseCommand created = service.getOrCreateBusinessConversation(
                player, owner, ConversationSubjectType.VENUE, venue);

        assertThat(created.getType()).isEqualTo(ConversationType.BUSINESS);
        assertThat(created.getSubjectId()).isEqualTo(venue);
        assertThat(created.getMembers()).extracting(ConversationResponseCommand.MemberItemResponse::getRole)
                .containsExactly(MemberRole.OWNER, MemberRole.MEMBER);

        Conversation existing = Conversation.builder()
                .conversationId(UUID.randomUUID())
                .type(ConversationType.BUSINESS)
                .subjectType(ConversationSubjectType.VENUE)
                .subjectId(venue)
                .members(new ArrayList<>(List.of(
                        ConversationMember.builder().userId(owner).role(MemberRole.OWNER).build(),
                        ConversationMember.builder().userId(player).role(MemberRole.MEMBER).build())))
                .build();
        when(conversationRepository.findPairOfType(ConversationType.BUSINESS, player, owner)).thenReturn(Optional.of(existing));

        ConversationResponseCommand reused = service.getOrCreateBusinessConversation(
                player, owner, ConversationSubjectType.TOURNAMENT, tournament);

        assertThat(reused.getConversationId()).isEqualTo(existing.getConversationId());
        assertThat(reused.getSubjectType()).isEqualTo(ConversationSubjectType.TOURNAMENT);
        assertThat(reused.getSubjectId()).isEqualTo(tournament);
    }

    @Test
    @DisplayName("chat công việc với người đã chặn mình hoặc với chính mình bị từ chối")
    void businessConversationRespectsBlocksAndSelf() {
        UUID player = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        when(socialBlockService.isBlocked(player, owner)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> service.getOrCreateBusinessConversation(
                player, owner, ConversationSubjectType.VENUE, UUID.randomUUID()))
                .isInstanceOf(ServiceException.class);
        Assertions.assertThatThrownBy(() -> service.getOrCreateBusinessConversation(
                player, player, ConversationSubjectType.VENUE, UUID.randomUUID()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("hộp thư hỗ trợ: chủ sân tạo một lần; admin mở thì được thêm vào với vai trò ADMIN, không thêm trùng")
    void supportConversationIsOnePerOwnerAndAdminsJoinOnOpen() throws ServiceException {
        UUID owner = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        when(conversationRepository.findSupportOf(owner)).thenReturn(Optional.empty());

        ConversationResponseCommand created = service.getOrCreateSupportConversation(owner, null);

        assertThat(created.getType()).isEqualTo(ConversationType.SUPPORT);
        assertThat(created.getName()).isEqualTo("Hỗ trợ GOAT Sports");
        assertThat(created.getMembers()).extracting(ConversationResponseCommand.MemberItemResponse::getUserId)
                .containsExactly(owner);

        Conversation existing = Conversation.builder()
                .conversationId(UUID.randomUUID())
                .type(ConversationType.SUPPORT)
                .members(new ArrayList<>(List.of(
                        ConversationMember.builder().userId(owner).role(MemberRole.OWNER).build())))
                .build();
        when(conversationRepository.findSupportOf(owner)).thenReturn(Optional.of(existing));

        ConversationResponseCommand opened = service.getOrCreateSupportConversation(owner, admin);
        assertThat(opened.getMembers()).extracting(ConversationResponseCommand.MemberItemResponse::getRole)
                .containsExactly(MemberRole.OWNER, MemberRole.ADMIN);

        existing.setMembers(new ArrayList<>(List.of(
                ConversationMember.builder().userId(owner).role(MemberRole.OWNER).build(),
                ConversationMember.builder().userId(admin).role(MemberRole.ADMIN).build())));
        Mockito.clearInvocations(conversationRepository);
        service.getOrCreateSupportConversation(owner, admin);
        Mockito.verify(conversationRepository, Mockito.never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("hộp thư hỗ trợ: tin cuối của chủ sân = chờ trả lời; tin mới được đẩy cho cả đội quản trị")
    void supportInboxFlagsAwaitingReplyAndBroadcastsNewMessages() throws ServiceException {
        UUID owner = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        UUID lastMessageId = UUID.randomUUID();
        Conversation support = Conversation.builder()
                .conversationId(UUID.randomUUID())
                .type(ConversationType.SUPPORT)
                .lastMessageId(lastMessageId)
                .members(new ArrayList<>(List.of(
                        ConversationMember.builder().userId(owner).role(MemberRole.OWNER).build())))
                .build();
        when(messageRepository.findById(lastMessageId)).thenReturn(Optional.of(
                Message.builder().messageId(lastMessageId).senderId(owner)
                        .content("Cần hỗ trợ").build()));
        when(conversationRepository.findByType(eq(ConversationType.SUPPORT), any()))
                .thenReturn(new PageImpl<>(List.of(support)));

        Page<ConversationResponseCommand> inbox = service.getSupportInbox(admin, Pageable.unpaged());
        assertThat(inbox.getContent()).singleElement()
                .satisfies(item -> assertThat(item.getAwaitingReply()).isTrue());

        when(conversationRepository.findById(support.getConversationId())).thenReturn(Optional.of(support));
        when(messageRepository.findBySenderIdAndClientMessageId(eq(owner), anyString())).thenReturn(Optional.empty());
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.sendMessage(SendMessageCommand.builder()
                .conversationId(support.getConversationId()).senderId(owner).content("Còn ai không?").build());
        Mockito.verify(webSocketPublisher).publishSupportMessage(any());
    }

    @Test
    void attachmentIsVisibleOnlyToActiveMembersOfALiveMessage() {
        UUID sender = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        Conversation conversation = directWith(sender, other);
        String key = "chat-messages/" + sender + "/abc-photo.png";
        Message message = Message.builder().conversationId(conversation.getConversationId()).deleted(false).build();
        when(messageRepository.findByAttachmentKey(key)).thenReturn(Optional.of(message));

        assertThat(service.canViewAttachment(other, key)).isTrue();
        assertThat(service.canViewAttachment(stranger, key)).isFalse();

        conversation.getMembers().stream().filter(member -> other.equals(member.getUserId()))
                .forEach(member -> member.setLeftAt(LocalDateTime.now()));
        assertThat(service.canViewAttachment(other, key)).isFalse();

        message.setDeleted(true);
        assertThat(service.canViewAttachment(sender, key)).isFalse();
        assertThat(service.canViewAttachment(sender, "chat-messages/unknown")).isFalse();
    }
}
