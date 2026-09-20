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

@RestController
@RequestMapping("/internal/conversations")
@RequiredArgsConstructor
public class SocialConversationInternalController {
    private final SocialChatService chatService;

    @PutMapping("/context/{contextId}")
    public ConversationResponseCommand syncContextConversation(
            @PathVariable java.util.UUID contextId,
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
}
