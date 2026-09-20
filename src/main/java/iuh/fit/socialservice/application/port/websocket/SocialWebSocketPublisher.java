package iuh.fit.socialservice.application.port.websocket;

import iuh.fit.socialservice.domain.entity.Message;
import iuh.fit.socialservice.application.dto.response.PresenceStatusCommand;

import java.util.UUID;

public interface SocialWebSocketPublisher {
    void publishMessageToConversation(UUID conversationId, Message message);

    void publishTypingIndicator(UUID conversationId, UUID userId, boolean typing);

    void publishPresenceStatus(PresenceStatusCommand status);
}
