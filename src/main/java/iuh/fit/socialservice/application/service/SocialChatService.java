package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
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
    Page<MessageResponseCommand> getConversationMessages(UUID conversationId, UUID userId, Pageable pageable) throws ServiceException;
    MessageResponseCommand sendMessage(SendMessageCommand command) throws ServiceException;
    void markAsRead(UUID conversationId, UUID userId) throws ServiceException;
}
