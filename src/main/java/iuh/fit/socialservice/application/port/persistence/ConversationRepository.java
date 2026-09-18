package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(UUID conversationId);
    Page<Conversation> findUserConversations(UUID userId, Pageable pageable);
    Optional<Conversation> findDirectBetween(UUID u1, UUID u2);
    Optional<Conversation> findByContextId(UUID contextId);
    Optional<Conversation> findPairConversation(UUID u1, UUID u2);
    java.util.List<Conversation> findAllPairConversations();
    void deleteById(UUID conversationId);
}
