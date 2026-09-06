package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.request.RespondFriendRequestCommand;
import iuh.fit.socialservice.application.dto.request.SendFriendRequestCommand;
import iuh.fit.socialservice.application.dto.response.FriendshipResponseCommand;
import iuh.fit.socialservice.presentation.request.RespondFriendRequest;
import iuh.fit.socialservice.presentation.request.SendFriendRequest;
import iuh.fit.socialservice.presentation.response.FriendshipResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SocialFriendConverter {

    public SendFriendRequestCommand toCommand(SendFriendRequest req) {
        return SendFriendRequestCommand.builder()
                .requesterId(req.getRequesterId())
                .addresseeId(req.getAddresseeId())
                .build();
    }

    public RespondFriendRequestCommand toCommand(UUID friendshipId, RespondFriendRequest req) {
        return RespondFriendRequestCommand.builder()
                .friendshipId(friendshipId)
                .actorUserId(req.getActorUserId())
                .accepted(req.isAccepted())
                .build();
    }

    public FriendshipResponse toResponse(FriendshipResponseCommand cmd) {
        return FriendshipResponse.builder()
                .friendshipId(cmd.getFriendshipId())
                .requesterId(cmd.getRequesterId())
                .addresseeId(cmd.getAddresseeId())
                .status(cmd.getStatus())
                .requestedAt(cmd.getRequestedAt())
                .respondedAt(cmd.getRespondedAt())
                .build();
    }
}
