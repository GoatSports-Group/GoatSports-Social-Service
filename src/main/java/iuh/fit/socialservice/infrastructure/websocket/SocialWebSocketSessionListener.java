package iuh.fit.socialservice.infrastructure.websocket;

import iuh.fit.socialservice.application.service.SocialPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class SocialWebSocketSessionListener {
    private final SocialPresenceService presenceService;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presenceService.disconnect(event.getSessionId(), null);
    }
}
