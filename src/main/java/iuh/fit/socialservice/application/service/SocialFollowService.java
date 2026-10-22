package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.domain.enums.PostSport;
import java.util.Set;
import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;

import iuh.fit.socialservice.application.port.persistence.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    List<FollowSuggestion> getFollowSuggestions(UUID viewerId, Set<PostSport> sports, int limit, int offset)
            throws ServiceException;

    /** Nguoi theo doi {@code userId} (cuon vo han), bo nguoi co quan he chan voi nguoi xem. */
    Page<FollowEntry> getFollowers(UUID userId, UUID viewerId,
                                                                   Pageable pageable);

    /** Nguoi {@code userId} dang theo doi (cuon vo han), bo nguoi co quan he chan voi nguoi xem. */
    Page<FollowEntry> getFollowing(UUID userId, UUID viewerId,
                                                                   Pageable pageable);

    record FollowEntry(UUID userId, LocalDateTime followedAt) {
    }

    record FollowSuggestion(UUID authorId, long postCount, String reason) {
    }
}
