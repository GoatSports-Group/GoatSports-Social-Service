package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageWindowCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.ConversationSubjectType;
import iuh.fit.socialservice.domain.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SocialChatService {
    Page<ConversationResponseCommand> getUserConversations(UUID userId, Pageable pageable);

    /** {@code types} rong / null = moi loai (Tin nhan ben client); admin dung BUSINESS + SUPPORT. */
    Page<ConversationResponseCommand> getUserConversations(
            UUID userId, Collection<ConversationType> types, Pageable pageable);

    /** Chat cong viec nguoi choi <-> chu san; dung lai doan chat cu cua cap nay va cap nhat doi tuong dang hoi. */
    ConversationResponseCommand getOrCreateBusinessConversation(
            UUID playerId, UUID ownerId, ConversationSubjectType subjectType, UUID subjectId)
            throws ServiceException;

    /**
     * Doan chat SUPPORT cua chu san {@code ownerId}. {@code adminId} khac null khi admin mo / chu dong nhan:
     * admin duoc them vao doan chat (vai tro ADMIN) neu chua o trong.
     */
    ConversationResponseCommand getOrCreateSupportConversation(UUID ownerId, UUID adminId) throws ServiceException;

    /** Hop thu chung cua doi quan tri: moi doan chat SUPPORT, moi nhat truoc. */
    Page<ConversationResponseCommand> getSupportInbox(UUID adminId, Pageable pageable);

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
    List<MessageResponseCommand> getMessagesByCursor(UUID conversationId, UUID userId,
                                                               LocalDateTime before,
                                                               LocalDateTime after, int size)
            throws ServiceException;

    /** Mo doan chat tai tin chua doc dau tien: vai tin truoc no lam ngu canh, roi tu no tro di. */
    MessageWindowCommand getUnreadWindow(
            UUID conversationId, UUID userId, int size) throws ServiceException;

    void markAsRead(UUID conversationId, UUID userId) throws ServiceException;

    /** Xoa doan chat phia nguoi nay: an lich su va an khoi danh sach den khi co tin moi; nguoi khac khong doi. */
    void clearConversation(UUID conversationId, UUID userId) throws ServiceException;

    void setMuted(UUID conversationId, UUID userId, boolean muted) throws ServiceException;

    /** Anh dinh kem {@code storageKey} nam trong tin chua bi xoa cua mot doan chat ma {@code userId} con la thanh vien. */
    boolean canViewAttachment(UUID userId, String storageKey);
}
