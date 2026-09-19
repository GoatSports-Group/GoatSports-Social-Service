package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.application.service.SocialFollowService;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialFollowServiceImpl implements SocialFollowService {
    private final FollowRepository followRepository;
    private final UserBlockRepository userBlockRepository;

    @Override
    @Transactional(readOnly = true)
    public VenueFollowStatusResponseCommand getVenueFollowStatus(UUID venueId, UUID followerId) {
        boolean followed = followerId != null && findFollow(followerId, FollowTargetType.VENUE, venueId) != null;
        return venueResponse(venueId, followed);
    }

    @Override
    @Transactional
    public VenueFollowStatusResponseCommand followVenue(UUID venueId, UUID followerId) {
        if (findFollow(followerId, FollowTargetType.VENUE, venueId) == null) {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .targetType(FollowTargetType.VENUE)
                    .targetId(venueId)
                    .build());
        }
        return venueResponse(venueId, true);
    }

    @Override
    @Transactional
    public VenueFollowStatusResponseCommand unfollowVenue(UUID venueId, UUID followerId) {
        Follow existing = findFollow(followerId, FollowTargetType.VENUE, venueId);
        if (existing != null) followRepository.deleteById(existing.getFollowId());
        return venueResponse(venueId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public UserFollowStatusResponseCommand getUserFollowStatus(UUID targetUserId, UUID followerId) {
        boolean followed = followerId != null
                && findFollow(followerId, FollowTargetType.USER, targetUserId) != null;
        return userResponse(targetUserId, followed);
    }

    @Override
    @Transactional
    public UserFollowStatusResponseCommand followUser(UUID targetUserId, UUID followerId)
            throws ServiceException {
        requireUsers(targetUserId, followerId);
        if (followerId.equals(targetUserId)) {
            throw new ServiceException("Bạn không thể theo dõi chính mình");
        }
        if (blockedEitherWay(followerId, targetUserId)) {
            throw new ServiceException("Không thể theo dõi người dùng này");
        }
        if (findFollow(followerId, FollowTargetType.USER, targetUserId) == null) {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .targetType(FollowTargetType.USER)
                    .targetId(targetUserId)
                    .build());
        }
        return userResponse(targetUserId, true);
    }

    @Override
    @Transactional
    public UserFollowStatusResponseCommand unfollowUser(UUID targetUserId, UUID followerId)
            throws ServiceException {
        requireUsers(targetUserId, followerId);
        Follow existing = findFollow(followerId, FollowTargetType.USER, targetUserId);
        if (existing != null) followRepository.deleteById(existing.getFollowId());
        return userResponse(targetUserId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getFollowingUserIds(UUID followerId) {
        if (followerId == null) return List.of();
        return followRepository.findByFollowerIdAndTargetType(followerId, FollowTargetType.USER).stream()
                .map(Follow::getTargetId)
                .toList();
    }

    private void requireUsers(UUID targetUserId, UUID followerId) throws ServiceException {
        if (followerId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        if (targetUserId == null) throw new ServiceException("Người dùng cần theo dõi không hợp lệ");
    }

    private boolean blockedEitherWay(UUID followerId, UUID targetUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedUserId(followerId, targetUserId)
                || userBlockRepository.existsByBlockerIdAndBlockedUserId(targetUserId, followerId);
    }

    private Follow findFollow(UUID followerId, FollowTargetType targetType, UUID targetId) {
        if (followerId == null || targetId == null) return null;
        return followRepository.findByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId)
                .orElse(null);
    }

    private VenueFollowStatusResponseCommand venueResponse(UUID venueId, boolean followed) {
        return VenueFollowStatusResponseCommand.builder()
                .venueId(venueId)
                .followed(followed)
                .build();
    }

    private UserFollowStatusResponseCommand userResponse(UUID targetUserId, boolean followed) {
        return UserFollowStatusResponseCommand.builder()
                .userId(targetUserId)
                .followed(followed)
                .followerCount(followRepository.countByTargetTypeAndTargetId(FollowTargetType.USER, targetUserId))
                .followingCount(followRepository
                        .findByFollowerIdAndTargetType(targetUserId, FollowTargetType.USER).size())
                .build();
    }
}
