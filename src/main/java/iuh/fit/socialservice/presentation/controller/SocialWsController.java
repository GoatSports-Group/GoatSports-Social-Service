package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.application.service.SocialPresenceService;
import iuh.fit.socialservice.domain.enums.MessageType;
import iuh.fit.socialservice.infrastructure.websocket.WebSocketUserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SocialWsController {

    private final SocialChatService chatService;
    private final SocialWebSocketPublisher webSocketPublisher;
    private final SocialPresenceService presenceService;

    @MessageMapping("/social/chat.send")
    public void handleSendMessage(@Payload WsMessagePayload payload, Principal principal) {
        UUID senderId = userIdOf(principal);
        if (senderId == null) {
            log.warn("Bỏ qua tin nhắn STOMP từ phiên không có danh tính");
            return;
        }
        try {
            chatService.sendMessage(SendMessageCommand.builder()
                    .conversationId(payload.getConversationId())
                    .senderId(senderId)
                    .clientMessageId(payload.getClientMessageId())
                    .content(payload.getContent())
                    .type(payload.getType() != null ? payload.getType() : MessageType.TEXT)
                    .replyToMessageId(payload.getReplyToMessageId())
                    .build());
        } catch (ServiceException exception) {
            // sendMessage đã tự chặn người ngoài cuộc trò chuyện; ở đây chỉ ghi lại.
            log.error("Không gửi được tin nhắn STOMP: {}", exception.getMessage());
        }
    }

    @MessageMapping("/social/chat.typing")
    public void handleTyping(@Payload WsTypingPayload payload, Principal principal) {
        UUID userId = userIdOf(principal);
        if (userId == null || payload.getConversationId() == null) return;
        webSocketPublisher.publishTypingIndicator(payload.getConversationId(), userId, payload.isTyping());
    }

    @MessageMapping("/social/presence.heartbeat")
    public void handlePresenceHeartbeat(SimpMessageHeaderAccessor headers, Principal principal) {
        UUID userId = userIdOf(principal);
        if (userId != null) {
            presenceService.heartbeat(userId, headers.getSessionId());
        }
    }

    @MessageMapping("/social/presence.offline")
    public void handlePresenceOffline(SimpMessageHeaderAccessor headers, Principal principal) {
        presenceService.disconnect(headers.getSessionId(), userIdOf(principal));
    }

    private UUID userIdOf(Principal principal) {
        return principal instanceof WebSocketUserPrincipal user ? user.userId() : null;
    }

    @Data
    public static class WsMessagePayload {
        private UUID conversationId;
        private String clientMessageId;
        private String content;
        private MessageType type;
        private UUID replyToMessageId;
    }

    @Data
    public static class WsTypingPayload {
        private UUID conversationId;
        private boolean typing;
    }
}
