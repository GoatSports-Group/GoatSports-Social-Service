package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.PresenceStatusCommand;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialPresenceServiceImplTest {
    @Mock
    private SocialWebSocketPublisher webSocketPublisher;

    private SocialPresenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocialPresenceServiceImpl(webSocketPublisher);
    }

    @Test
    void heartbeatPublishesOnlineOnlyOnFirstSession() {
        UUID userId = UUID.randomUUID();

        service.heartbeat(userId, "session-1");
        reset(webSocketPublisher);
        service.heartbeat(userId, "session-1");

        verify(webSocketPublisher, never()).publishPresenceStatus(org.mockito.ArgumentMatchers.any());
        assertThat(service.getPresence(List.of(userId)).getFirst().isOnline()).isTrue();
    }

    @Test
    void disconnectPublishesOfflineAfterLastSession() {
        UUID userId = UUID.randomUUID();
        service.heartbeat(userId, "session-1");
        service.heartbeat(userId, "session-2");
        reset(webSocketPublisher);

        service.disconnect("session-1", userId);
        verify(webSocketPublisher, never()).publishPresenceStatus(org.mockito.ArgumentMatchers.any());

        service.disconnect("session-2", userId);
        ArgumentCaptor<PresenceStatusCommand> captor = ArgumentCaptor.forClass(PresenceStatusCommand.class);
        verify(webSocketPublisher).publishPresenceStatus(captor.capture());
        assertThat(captor.getValue().isOnline()).isFalse();
        assertThat(captor.getValue().getLastSeenAt()).isNotNull();
    }
}
