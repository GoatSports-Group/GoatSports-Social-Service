package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SocialChatService {
    Page<ConversationResponseCommand> getUserConversations(UUID userId, Pageable pageable);

    ConversationResponseCommand getConversation(UUID conversationId, UUID userId) throws ServiceException;

    ConversationResponseCommand getOrCreateDirectConversation(UUID user1Id, UUID user2Id) throws ServiceException;

    ConversationResponseCommand createGroupConversation(CreateConversationCommand command) throws ServiceException;

    ConversationResponseCommand syncContextConversation(SyncContextConversationCommand command) throws ServiceException;

    Page<MessageResponseCommand> getConversationMessages(UUID conversationId, UUID userId, Pageable pageable) throws ServiceException;

    MessageResponseCommand sendMessage(SendMessageCommand command) throws ServiceException;

    /**
     * Cuon hai chieu theo con tro thoi gian (moi nhat truoc). {@code after} co gia tri thi lay tin moi hon,
     * khong thi lay tin cu hon {@code before} (null = tu moi nhat).
     */
    java.util.List<MessageResponseCommand> getMessagesByCursor(UUID conversationId, UUID userId,
                                                               java.time.LocalDateTime before,
                                                               java.time.LocalDateTime after, int size)
            throws ServiceException;

    /** Mo doan chat tai tin chua doc dau tien: vai tin truoc no lam ngu canh, roi tu no tro di. */
    iuh.fit.socialservice.application.dto.response.MessageWindowCommand getUnreadWindow(
            UUID conversationId, UUID userId, int size) throws ServiceException;

    void markAsRead(UUID conversationId, UUID userId) throws ServiceException;

    /** Xoa doan chat phia nguoi nay: an lich su va an khoi danh sach den khi co tin moi; nguoi khac khong doi. */
    void clearConversation(UUID conversationId, UUID userId) throws ServiceException;

    void setMuted(UUID conversationId, UUID userId, boolean muted) throws ServiceException;
}
