package iuh.fit.socialservice.application.port.websocket;

import iuh.fit.socialservice.domain.entity.Message;

import java.util.UUID;

public interface SocialWebSocketPublisher {
    void publishMessageToConversation(UUID conversationId, Message message);
    void publishTypingIndicator(UUID conversationId, UUID userId, String userName, boolean typing);
}
