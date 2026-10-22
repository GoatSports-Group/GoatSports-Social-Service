package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(UUID messageId);
    Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, String clientMessageId);
    /** {@code after} (co the null): chi tin gui sau moc nay, dung cho doan chat da xoa phia nguoi xem. */
    Page<Message> findByConversationId(UUID conversationId, LocalDateTime after, Pageable pageable);
    Optional<Message> findLatestByConversationId(UUID conversationId);
    Optional<Message> findByAttachmentKey(String storageKey);

    /** Tin cu hon {@code before} (va moi hon {@code floor} neu co), moi nhat truoc. */
    List<Message> findOlder(UUID conversationId, LocalDateTime before,
                                      LocalDateTime floor, int limit);

    /** Tin moi hon {@code after} (gom chinh moc neu {@code inclusive}), cu nhat truoc. */
    List<Message> findNewer(UUID conversationId, LocalDateTime after, boolean inclusive,
                                      LocalDateTime floor, int limit);

    /** Tin dau tien nguoi khac gui ma {@code userId} chua doc (sau {@code floor} neu co). */
    Optional<Message> findFirstUnread(UUID conversationId, UUID userId, LocalDateTime floor);
    void markConversationAsRead(UUID conversationId, UUID userId);
    long countUnreadMessages(UUID conversationId, UUID userId);
    void deleteById(UUID messageId);
    long reassignConversation(UUID fromConversationId, UUID toConversationId);
}
