package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.RespondFriendRequestCommand;
import iuh.fit.socialservice.application.dto.request.SendFriendRequestCommand;
import iuh.fit.socialservice.application.dto.response.FriendshipResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public interface SocialFriendService {
    FriendshipResponseCommand sendFriendRequest(SendFriendRequestCommand command) throws ServiceException;
    FriendshipResponseCommand respondFriendRequest(RespondFriendRequestCommand command) throws ServiceException;
    void unfriend(UUID friendshipId, UUID actorUserId) throws ServiceException;
    List<FriendshipResponseCommand> getFriendsList(UUID userId);
    List<FriendshipResponseCommand> getPendingReceivedRequests(UUID userId);
    List<FriendshipResponseCommand> getPendingSentRequests(UUID userId);
}
