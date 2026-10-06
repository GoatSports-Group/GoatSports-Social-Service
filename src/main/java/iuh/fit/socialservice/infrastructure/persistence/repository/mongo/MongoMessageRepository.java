package iuh.fit.socialservice.infrastructure.persistence.repository.mongo;

import iuh.fit.socialservice.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MongoMessageRepository extends MongoRepository<MessageEntity, String> {

    Page<MessageEntity> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);

    Page<MessageEntity> findByConversationIdAndSentAtAfterOrderBySentAtDesc(
            String conversationId, java.time.LocalDateTime after, Pageable pageable);

    List<MessageEntity> findByConversationIdOrderBySentAtAsc(String conversationId);

    Optional<MessageEntity> findFirstByConversationIdOrderBySentAtDesc(String conversationId);

    Optional<MessageEntity> findBySenderIdAndClientMessageId(String senderId, String clientMessageId);
}
