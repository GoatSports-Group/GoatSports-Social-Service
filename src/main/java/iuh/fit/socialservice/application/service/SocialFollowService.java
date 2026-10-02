package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;

import iuh.fit.socialservice.application.port.persistence.PostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SocialFollowService {
    VenueFollowStatusResponseCommand getVenueFollowStatus(UUID venueId, UUID followerId);
    VenueFollowStatusResponseCommand followVenue(UUID venueId, UUID followerId);
    VenueFollowStatusResponseCommand unfollowVenue(UUID venueId, UUID followerId);

    /** San nguoi dung da luu, moi luu truoc. Chi tra id: thong tin san lay tu venue-service. */
    List<SavedVenue> getSavedVenues(UUID followerId);

    record SavedVenue(UUID venueId, LocalDateTime savedAt) {}

    UserFollowStatusResponseCommand getUserFollowStatus(UUID targetUserId, UUID followerId);
    UserFollowStatusResponseCommand followUser(UUID targetUserId, UUID followerId) throws ServiceException;
    UserFollowStatusResponseCommand unfollowUser(UUID targetUserId, UUID followerId) throws ServiceException;

    List<UUID> getFollowingUserIds(UUID followerId);

    /** Tac gia dang dang bai cong khai nhieu, chua duoc theo doi, khong phai ban va khong bi chan. */
    List<PostRepository.AuthorActivity> getFollowSuggestions(UUID viewerId, int limit) throws ServiceException;
}
