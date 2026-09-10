package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialChatConverter;
import iuh.fit.socialservice.presentation.request.CreateConversationRequest;
import iuh.fit.socialservice.presentation.request.SendMessageRequest;
import iuh.fit.socialservice.presentation.response.ConversationResponse;
import iuh.fit.socialservice.presentation.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/conversations")
@RequiredArgsConstructor
public class SocialChatController {

    private final SocialChatService chatService;
    private final SocialChatConverter chatConverter;

    @GetMapping
    @ApiMessage("Lấy danh sách cuộc trò chuyện thành công")
    public ResponseEntity<Page<ConversationResponse>> getUserConversations(
            @RequestParam UUID userId,
            Pageable pageable
    ) {
        Page<ConversationResponseCommand> result = chatService.getUserConversations(userId, pageable);
        return ResponseEntity.ok(result.map(chatConverter::toResponse));
    }

    @GetMapping("/{conversationId}")
    @ApiMessage("Lấy thông tin cuộc trò chuyện thành công")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID conversationId,
            @RequestParam UUID userId
    ) throws ServiceException {
        ConversationResponseCommand result = chatService.getConversation(conversationId, userId);
        return ResponseEntity.ok(chatConverter.toResponse(result));
    }

    @PostMapping("/direct")
    @ApiMessage("Lấy hoặc tạo cuộc trò chuyện trực tiếp 1-1 thành công")
    public ResponseEntity<ConversationResponse> getOrCreateDirectConversation(
            @RequestParam UUID user1Id,
            @RequestParam UUID user2Id
    ) throws ServiceException {
        ConversationResponseCommand result = chatService.getOrCreateDirectConversation(user1Id, user2Id);
        return ResponseEntity.ok(chatConverter.toResponse(result));
    }

    @PostMapping("/group")
    @ApiMessage("Tạo nhóm trò chuyện thành công")
    public ResponseEntity<ConversationResponse> createGroupConversation(
            @Valid @RequestBody CreateConversationRequest request
    ) throws ServiceException {
        CreateConversationCommand cmd = chatConverter.toCommand(request);
        ConversationResponseCommand result = chatService.createGroupConversation(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatConverter.toResponse(result));
    }

    @GetMapping("/{conversationId}/messages")
    @ApiMessage("Lấy lịch sử tin nhắn cuộc trò chuyện thành công")
    public ResponseEntity<Page<MessageResponse>> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam UUID userId,
            Pageable pageable
    ) throws ServiceException {
        Page<MessageResponseCommand> result = chatService.getConversationMessages(conversationId, userId, pageable);
        return ResponseEntity.ok(result.map(chatConverter::toResponse));
    }

    @PostMapping("/{conversationId}/messages")
    @ApiMessage("Gửi tin nhắn thành công")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) throws ServiceException {
        SendMessageCommand cmd = chatConverter.toCommand(conversationId, request);
        MessageResponseCommand result = chatService.sendMessage(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatConverter.toResponse(result));
    }

    @PutMapping("/{conversationId}/read")
    @ApiMessage("Đánh dấu đã đọc thành công")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestParam UUID userId
    ) throws ServiceException {
        chatService.markAsRead(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
}
