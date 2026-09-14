package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.enums.ConversationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
        when(conversationRepository.save(any(Conversation.class)))
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
    void createDirectConversationLeavesGeneratedMemberIdsUnset() throws ServiceException {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        service.getOrCreateDirectConversation(firstUserId, secondUserId);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        org.mockito.Mockito.verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getMembers())
                .allSatisfy(member -> assertThat(member.getMemberId()).isNull());
    }
}
