package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.presentation.converter.SocialChatConverter;
import iuh.fit.socialservice.presentation.request.SendMessageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Danh tinh lay tu phien dang nhap; id client tu gui khong the thay nguoi khac. */
class SocialChatControllerTest {
    private final SocialChatService chatService = mock(SocialChatService.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final SocialChatController controller =
            new SocialChatController(chatService, new SocialChatConverter(), currentUser);
    private final UUID me = UUID.randomUUID();

    @Test
    void readingAnotherUsersConversationsIsRefused() {
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(me.toString()));

        assertThatThrownBy(() -> controller.getUserConversations(UUID.randomUUID(), Pageable.unpaged()))
                .isInstanceOf(ServiceException.class);
        verify(chatService, never()).getUserConversations(any(), any());
    }

    @Test
    void theSenderIsAlwaysTheSignedInUser() throws Exception {
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(me.toString()));
        when(chatService.getUserConversations(eq(me), any())).thenReturn(Page.empty());
        SendMessageRequest spoofed = new SendMessageRequest();
        spoofed.setSenderId(UUID.randomUUID());
        spoofed.setContent("hi");

        assertThatThrownBy(() -> controller.sendMessage(UUID.randomUUID(), spoofed)).isInstanceOf(ServiceException.class);

        controller.getUserConversations(null, Pageable.unpaged());
        verify(chatService).getUserConversations(eq(me), any());
        verify(chatService, never()).sendMessage(argThat(command -> !me.equals(command.getSenderId())));
    }

    @Test
    void anonymousCallsAreRejected() {
        when(currentUser.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.markAsRead(UUID.randomUUID(), null)).isInstanceOf(UnauthenticatedException.class);
    }
}
