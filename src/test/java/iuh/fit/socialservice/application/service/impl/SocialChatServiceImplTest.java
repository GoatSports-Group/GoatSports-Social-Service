package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import iuh.fit.socialservice.domain.enums.MemberRole;
import iuh.fit.socialservice.domain.enums.ConversationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private SocialChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocialChatServiceImpl(
                conversationRepository,
                messageRepository,
                socialBlockService,
                webSocketPublisher
        );
        org.mockito.Mockito.lenient().when(conversationRepository.save(any(Conversation.class)))
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
        org.mockito.Mockito.verify(conversationRepository).save(captor.capture());
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
                .thenReturn(java.util.Optional.of(existing));

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
        org.mockito.Mockito.verify(conversationRepository).save(captor.capture());
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
        when(conversationRepository.findByContextId(sessionId)).thenReturn(java.util.Optional.of(
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
        org.mockito.Mockito.verify(conversationRepository, org.mockito.Mockito.never()).save(any(Conversation.class));
    }

    @Test
    void createDirectConversationLeavesGeneratedMemberIdsUnset() throws ServiceException {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        service.getOrCreateDirectConversation(firstUserId, secondUserId);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        org.mockito.Mockito.verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getMembers())
                .allSatisfy(member -> assertThat(member.getMemberId()).isNull());
    }

    private Conversation clubConversation(UUID contextId, UUID ownerId, List<UUID> memberIds) {
        LocalDateTime now = LocalDateTime.now();
        List<ConversationMember> members = new java.util.ArrayList<>();
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
        org.mockito.Mockito.verify(conversationRepository).save(captor.capture());
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
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.syncContextConversation(SyncContextConversationCommand.builder()
                        .contextId(UUID.randomUUID()).name("CLB test").build()))
                .isInstanceOf(ServiceException.class);
    }
}
