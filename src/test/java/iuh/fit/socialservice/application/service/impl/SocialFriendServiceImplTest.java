package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.RespondFriendRequestCommand;
import iuh.fit.socialservice.application.dto.request.SendFriendRequestCommand;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialFriendServiceImplTest {
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private SocialBlockService blockService;
    @Mock private SocialEventPublisher eventPublisher;

    private SocialFriendServiceImpl service;
    private final UUID requester = UUID.randomUUID();
    private final UUID addressee = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SocialFriendServiceImpl(friendshipRepository, blockService, eventPublisher);
    }

    private Friendship pending() {
        return Friendship.builder().friendshipId(UUID.randomUUID()).requesterId(requester).addresseeId(addressee)
                .status(FriendshipStatus.PENDING).build();
    }

    @Test
    void sendingARequestNotifiesTheAddressee() throws Exception {
        when(friendshipRepository.findBetweenUsers(requester, addressee)).thenReturn(Optional.empty());
        when(friendshipRepository.save(any())).thenAnswer(call -> {
            Friendship saved = call.getArgument(0);
            saved.setFriendshipId(UUID.randomUUID());
            return saved;
        });

        service.sendFriendRequest(SendFriendRequestCommand.builder().requesterId(requester).addresseeId(addressee).build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.FRIEND_REQUESTED), eq(addressee), eq(requester), any(),
                anyMap(), any());
    }

    @Test
    void acceptingNotifiesTheRequesterButRejectingStaysSilent() throws Exception {
        Friendship accepted = pending();
        Friendship rejected = pending();
        when(friendshipRepository.findById(accepted.getFriendshipId())).thenReturn(Optional.of(accepted));
        when(friendshipRepository.findById(rejected.getFriendshipId())).thenReturn(Optional.of(rejected));
        when(friendshipRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.respondFriendRequest(RespondFriendRequestCommand.builder()
                .friendshipId(accepted.getFriendshipId()).actorUserId(addressee).accepted(true).build());
        service.respondFriendRequest(RespondFriendRequestCommand.builder()
                .friendshipId(rejected.getFriendshipId()).actorUserId(addressee).accepted(false).build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.FRIEND_ACCEPTED), eq(requester), eq(addressee),
                eq(accepted.getFriendshipId()), anyMap(), any());
        verify(eventPublisher, never()).publish(anyString(), any(), any(), eq(rejected.getFriendshipId()), anyMap(), any());
    }
}
