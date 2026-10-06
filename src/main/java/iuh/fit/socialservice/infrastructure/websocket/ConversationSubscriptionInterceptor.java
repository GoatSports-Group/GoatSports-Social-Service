package iuh.fit.socialservice.infrastructure.websocket;

import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationSubscriptionInterceptor implements ChannelInterceptor {

    private static final String CONVERSATION_PREFIX = "/topic/conversations/";
    private static final String USER_PREFIX = "/topic/users/";

    private final ConversationRepository conversationRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith(USER_PREFIX)) {
            UUID owner = idAfter(destination, USER_PREFIX);
            UUID userId = userIdOf(accessor.getUser());
            if (owner == null || !owner.equals(userId)) {
                log.warn("Từ chối subscribe {} cho người dùng {}", destination, userId);
                throw new IllegalArgumentException("Bạn không thể nghe kênh của người khác.");
            }
            return message;
        }
        if (destination == null || !destination.startsWith(CONVERSATION_PREFIX)) {
            return message;
        }

        UUID conversationId = conversationIdOf(destination);
        UUID userId = userIdOf(accessor.getUser());
        if (conversationId == null || userId == null || !isActiveMember(conversationId, userId)) {
            log.warn("Từ chối subscribe {} cho người dùng {}", destination, userId);
            throw new IllegalArgumentException("Bạn không thuộc cuộc trò chuyện này.");
        }
        return message;
    }

    /** Vừa khớp `/topic/conversations/{id}` vừa khớp `/topic/conversations/{id}/typing`. */
    private UUID conversationIdOf(String destination) {
        return idAfter(destination, CONVERSATION_PREFIX);
    }

    private UUID idAfter(String destination, String prefix) {
        String remainder = destination.substring(prefix.length());
        int slash = remainder.indexOf('/');
        String raw = slash < 0 ? remainder : remainder.substring(0, slash);
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UUID userIdOf(Principal principal) {
        return principal instanceof WebSocketUserPrincipal user ? user.userId() : null;
    }

    private boolean isActiveMember(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) return false;
        List<ConversationMember> members = conversation.getMembers();
        if (members == null) return false;
        return members.stream()
                .filter(Objects::nonNull)
                .anyMatch(member -> userId.equals(member.getUserId()) && member.getLeftAt() == null);
    }
}
