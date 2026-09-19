package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public interface SocialFollowService {
    VenueFollowStatusResponseCommand getVenueFollowStatus(UUID venueId, UUID followerId);
    VenueFollowStatusResponseCommand followVenue(UUID venueId, UUID followerId);
    VenueFollowStatusResponseCommand unfollowVenue(UUID venueId, UUID followerId);

    UserFollowStatusResponseCommand getUserFollowStatus(UUID targetUserId, UUID followerId);
    UserFollowStatusResponseCommand followUser(UUID targetUserId, UUID followerId) throws ServiceException;
    UserFollowStatusResponseCommand unfollowUser(UUID targetUserId, UUID followerId) throws ServiceException;

    List<UUID> getFollowingUserIds(UUID followerId);
}
