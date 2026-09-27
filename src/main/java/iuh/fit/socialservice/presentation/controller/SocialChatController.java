package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.request.CreateConversationCommand;
import iuh.fit.socialservice.application.dto.request.SendMessageCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.dto.response.MessageResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialChatConverter;
import iuh.fit.socialservice.presentation.request.CreateConversationRequest;
import iuh.fit.socialservice.presentation.request.SendMessageRequest;
import iuh.fit.socialservice.presentation.response.ConversationResponse;
import iuh.fit.socialservice.presentation.response.MessageResponse;
import iuh.fit.socialservice.presentation.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Danh tinh luon lay tu phien dang nhap (header gateway), khong tu tham so client gui. Cac tham so
 * userId / user1Id / senderId / creatorId cu van duoc nhan de khong vo client, nhung phai trung voi
 * nguoi dang goi — truoc day ai biet id nguoi khac la doc duoc hoi thoai va gui tin duoi ten ho.
 */
@RestController
@RequestMapping("/api/v1/social/conversations")
@RequiredArgsConstructor
public class SocialChatController {

    private final SocialChatService chatService;
    private final SocialChatConverter chatConverter;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @ApiMessage("Lấy danh sách cuộc trò chuyện thành công")
    public ResponseEntity<PageResponse<ConversationResponse>> getUserConversations(
            @RequestParam(required = false) UUID userId,
            Pageable pageable
    ) throws ServiceException {
        Page<ConversationResponseCommand> result = chatService.getUserConversations(caller(userId), pageable);
        return ResponseEntity.ok(PageResponse.from(result.map(chatConverter::toResponse)));
    }

    @GetMapping("/{conversationId}")
    @ApiMessage("Lấy thông tin cuộc trò chuyện thành công")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID userId
    ) throws ServiceException {
        ConversationResponseCommand result = chatService.getConversation(conversationId, caller(userId));
        return ResponseEntity.ok(chatConverter.toResponse(result));
    }

    @PostMapping("/direct")
    @ApiMessage("Lấy hoặc tạo cuộc trò chuyện trực tiếp 1-1 thành công")
    public ResponseEntity<ConversationResponse> getOrCreateDirectConversation(
            @RequestParam(required = false) UUID user1Id,
            @RequestParam UUID user2Id
    ) throws ServiceException {
        ConversationResponseCommand result = chatService.getOrCreateDirectConversation(caller(user1Id), user2Id);
        return ResponseEntity.ok(chatConverter.toResponse(result));
    }

    @PostMapping("/group")
    @ApiMessage("Tạo nhóm trò chuyện thành công")
    public ResponseEntity<ConversationResponse> createGroupConversation(
            @Valid @RequestBody CreateConversationRequest request
    ) throws ServiceException {
        request.setCreatorId(caller(request.getCreatorId()));
        CreateConversationCommand cmd = chatConverter.toCommand(request);
        ConversationResponseCommand result = chatService.createGroupConversation(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatConverter.toResponse(result));
    }

    @GetMapping("/{conversationId}/messages")
    @ApiMessage("Lấy lịch sử tin nhắn cuộc trò chuyện thành công")
    public ResponseEntity<PageResponse<MessageResponse>> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID userId,
            Pageable pageable
    ) throws ServiceException {
        Page<MessageResponseCommand> result = chatService.getConversationMessages(conversationId, caller(userId), pageable);
        return ResponseEntity.ok(PageResponse.from(result.map(chatConverter::toResponse)));
    }

    @PostMapping("/{conversationId}/messages")
    @ApiMessage("Gửi tin nhắn thành công")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) throws ServiceException {
        request.setSenderId(caller(request.getSenderId()));
        SendMessageCommand cmd = chatConverter.toCommand(conversationId, request);
        MessageResponseCommand result = chatService.sendMessage(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatConverter.toResponse(result));
    }

    @PutMapping("/{conversationId}/read")
    @ApiMessage("Đánh dấu đã đọc thành công")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID userId
    ) throws ServiceException {
        chatService.markAsRead(conversationId, caller(userId));
        return ResponseEntity.noContent().build();
    }

    /** Nguoi dang goi; neu client con gui kem mot id thi id do phai la chinh ho. */
    private UUID caller(UUID claimed) throws ServiceException {
        UUID me = currentUserProvider.getCurrentUserId()
                .map(this::uuidOrNull)
                .orElse(null);
        if (me == null) throw new UnauthenticatedException("Phiên đăng nhập đã hết hạn");
        if (claimed != null && !claimed.equals(me)) {
            throw new ServiceException("Bạn không thể thao tác thay người dùng khác.");
        }
        return me;
    }

    private UUID uuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
