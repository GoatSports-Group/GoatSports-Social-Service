package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(UUID messageId);
    Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, String clientMessageId);
    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);
    Optional<Message> findLatestByConversationId(UUID conversationId);
    void markConversationAsRead(UUID conversationId, UUID userId);
    long countUnreadMessages(UUID conversationId, UUID userId);
    void deleteById(UUID messageId);
}
