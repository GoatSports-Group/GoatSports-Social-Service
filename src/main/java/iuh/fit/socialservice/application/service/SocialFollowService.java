package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;

import java.util.UUID;

public interface SocialFollowService {
    VenueFollowStatusResponseCommand getVenueFollowStatus(UUID venueId, UUID followerId);
    VenueFollowStatusResponseCommand followVenue(UUID venueId, UUID followerId);
    VenueFollowStatusResponseCommand unfollowVenue(UUID venueId, UUID followerId);
}
