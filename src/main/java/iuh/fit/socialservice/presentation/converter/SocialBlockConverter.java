package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.request.BlockUserCommand;
import iuh.fit.socialservice.application.dto.response.UserBlockResponseCommand;
import iuh.fit.socialservice.presentation.request.BlockUserRequest;
import iuh.fit.socialservice.presentation.response.UserBlockResponse;
import org.springframework.stereotype.Component;

@Component
public class SocialBlockConverter {

    public BlockUserCommand toCommand(BlockUserRequest req) {
        return BlockUserCommand.builder()
                .blockerId(req.getBlockerId())
                .blockedUserId(req.getBlockedUserId())
                .reason(req.getReason())
                .build();
    }

    public UserBlockResponse toResponse(UserBlockResponseCommand cmd) {
        return UserBlockResponse.builder()
                .blockId(cmd.getBlockId())
                .blockerId(cmd.getBlockerId())
                .blockedUserId(cmd.getBlockedUserId())
                .reason(cmd.getReason())
                .blockedAt(cmd.getBlockedAt())
                .build();
    }
}
