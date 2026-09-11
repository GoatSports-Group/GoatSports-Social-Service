package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.service.SocialFollowService;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialFollowServiceImpl implements SocialFollowService {
    private final FollowRepository followRepository;

    @Override
    @Transactional(readOnly = true)
    public VenueFollowStatusResponseCommand getVenueFollowStatus(UUID venueId, UUID followerId) {
        boolean followed = followerId != null && findVenueFollow(followerId, venueId) != null;
        return response(venueId, followed);
    }

    @Override
    @Transactional
    public VenueFollowStatusResponseCommand followVenue(UUID venueId, UUID followerId) {
        Follow existing = findVenueFollow(followerId, venueId);
        if (existing == null) {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .targetType(FollowTargetType.VENUE)
                    .targetId(venueId)
                    .build());
        }
        return response(venueId, true);
    }

    @Override
    @Transactional
    public VenueFollowStatusResponseCommand unfollowVenue(UUID venueId, UUID followerId) {
        Follow existing = findVenueFollow(followerId, venueId);
        if (existing != null) followRepository.deleteById(existing.getFollowId());
        return response(venueId, false);
    }

    private Follow findVenueFollow(UUID followerId, UUID venueId) {
        return followRepository.findByFollowerIdAndTargetTypeAndTargetId(
                followerId,
                FollowTargetType.VENUE,
                venueId
        ).orElse(null);
    }

    private VenueFollowStatusResponseCommand response(UUID venueId, boolean followed) {
        return VenueFollowStatusResponseCommand.builder()
                .venueId(venueId)
                .followed(followed)
                .build();
    }
}
