package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {
    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID conversationId);

    Page<Conversation> findUserConversations(UUID userId, Pageable pageable);

    /** {@code types} rong / null = moi loai. */
    Page<Conversation> findUserConversations(UUID userId, Collection<ConversationType> types,
                                             Pageable pageable);
    Page<Conversation> findByType(ConversationType type, Pageable pageable);
    Optional<Conversation> findDirectBetween(UUID u1, UUID u2);
    /** Doan chat hai nguoi dung loai {@code type} giua u1 va u2. */
    Optional<Conversation> findPairOfType(ConversationType type, UUID u1, UUID u2);
    /** Doan chat SUPPORT cua chu san {@code ownerId}. */
    Optional<Conversation> findSupportOf(UUID ownerId);

    Optional<Conversation> findByContextId(UUID contextId);
    Optional<Conversation> findPairConversation(UUID u1, UUID u2);

    List<Conversation> findAllPairConversations();

    List<CoMember> findCoMembers(UUID userId, Collection<ConversationType> types);

    void deleteById(UUID conversationId);

    record CoMember(UUID userId, ConversationType type, String conversationName) {
    }
}
