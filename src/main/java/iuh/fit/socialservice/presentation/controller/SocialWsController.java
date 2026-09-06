package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.websocket.SocialWebSocketPublisher;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.domain.enums.MessageType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SocialWsController {

    private final SocialChatService chatService;
    private final SocialWebSocketPublisher webSocketPublisher;

    @MessageMapping("/social/chat.send")
    public void handleSendMessage(@Payload WsMessagePayload payload) {
        try {
            SendMessageCommand cmd = SendMessageCommand.builder()
                    .conversationId(payload.getConversationId())
                    .senderId(payload.getSenderId())
                    .clientMessageId(payload.getClientMessageId())
                    .content(payload.getContent())
                    .type(payload.getType() != null ? payload.getType() : MessageType.TEXT)
                    .replyToMessageId(payload.getReplyToMessageId())
                    .build();

            chatService.sendMessage(cmd);
        } catch (ServiceException e) {
            log.error("Failed to handle STOMP chat message: {}", e.getMessage());
        }
    }

    @MessageMapping("/social/chat.typing")
    public void handleTyping(@Payload WsTypingPayload payload) {
        webSocketPublisher.publishTypingIndicator(
                payload.getConversationId(),
                payload.getUserId(),
                payload.getUserName(),
                payload.isTyping()
        );
    }

    @Data
    public static class WsMessagePayload {
        private UUID conversationId;
        private UUID senderId;
        private String clientMessageId;
        private String content;
        private MessageType type;
        private UUID replyToMessageId;
    }

    @Data
    public static class WsTypingPayload {
        private UUID conversationId;
        private UUID userId;
        private String userName;
        private boolean typing;
    }
}
