package iuh.fit.socialservice.infrastructure.websocket;

import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.domain.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialWebSocketPublisherImpl implements SocialWebSocketPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishMessageToConversation(UUID conversationId, Message message) {
        String destination = "/topic/conversations/" + conversationId;
        log.info("Publishing social message to {}", destination);
        messagingTemplate.convertAndSend(destination, (Object) message);
    }

    @Override
    public void publishTypingIndicator(UUID conversationId, UUID userId, String userName, boolean typing) {
        String destination = "/topic/conversations/" + conversationId + "/typing";
        Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "userName", userName,
                "typing", typing
        );
        messagingTemplate.convertAndSend(destination, (Object) payload);
    }
}
