package iuh.fit.socialservice.presentation.controller.internal;

import iuh.fit.socialservice.application.dto.request.SyncContextConversationCommand;
import iuh.fit.socialservice.application.dto.response.ConversationResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialChatService;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.presentation.request.SyncContextConversationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/conversations")
@RequiredArgsConstructor
public class SocialConversationInternalController {
    private final SocialChatService chatService;

    @PutMapping("/context/{contextId}")
    public ConversationResponseCommand syncContextConversation(
            @PathVariable UUID contextId,
            @Valid @RequestBody SyncContextConversationRequest request
    ) throws ServiceException {
        return chatService.syncContextConversation(SyncContextConversationCommand.builder()
                .contextId(contextId)
                .type(request.getType() == null ? ConversationType.CLUB : request.getType())
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .ownerId(request.getOwnerId())
                .memberIds(request.getMemberIds())
                .build());
    }

    /** storage-service hoi truoc khi cap URL doc anh trong thu muc rieng tu chat-messages. */
    @GetMapping("/attachments/access")
    public boolean canViewAttachment(@RequestParam UUID userId, @RequestParam String key) {
        return chatService.canViewAttachment(userId, key);
    }
}
