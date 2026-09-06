package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.BlockUserCommand;
import iuh.fit.socialservice.application.dto.response.UserBlockResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public interface SocialBlockService {
    UserBlockResponseCommand blockUser(BlockUserCommand command) throws ServiceException;
    void unblockUser(UUID blockId, UUID actorUserId) throws ServiceException;
    List<UserBlockResponseCommand> getBlockedUsers(UUID userId);
    boolean isBlocked(UUID u1, UUID u2);
}
