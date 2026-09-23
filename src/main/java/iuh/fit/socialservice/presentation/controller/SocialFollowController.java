package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialFollowService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialFollowConverter;
import iuh.fit.socialservice.presentation.response.VenueFollowStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/follows/venues")
@RequiredArgsConstructor
public class SocialFollowController {
    private final SocialFollowService followService;
    private final SocialFollowConverter followConverter;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{venueId}")
    @ApiMessage("Lấy trạng thái lưu sân thành công")
    public ResponseEntity<VenueFollowStatusResponse> getStatus(@PathVariable UUID venueId) {
        return ResponseEntity.ok(followConverter.toResponse(
                followService.getVenueFollowStatus(venueId, currentUserIdOrNull())
        ));
    }

    @PostMapping("/{venueId}")
    @ApiMessage("Lưu sân thành công")
    public ResponseEntity<VenueFollowStatusResponse> followVenue(
            @PathVariable UUID venueId
    ) throws ServiceException {
        return ResponseEntity.ok(followConverter.toResponse(
                followService.followVenue(venueId, requiredUserId())
        ));
    }

    @DeleteMapping("/{venueId}")
    @ApiMessage("Bỏ lưu sân thành công")
    public ResponseEntity<VenueFollowStatusResponse> unfollowVenue(
            @PathVariable UUID venueId
    ) throws ServiceException {
        return ResponseEntity.ok(followConverter.toResponse(
                followService.unfollowVenue(venueId, requiredUserId())
        ));
    }

    private UUID requiredUserId() throws ServiceException {
        UUID userId = currentUserIdOrNull();
        if (userId == null) throw new UnauthenticatedException("Phiên đăng nhập đã hết hạn");
        return userId;
    }

    private UUID currentUserIdOrNull() {
        return currentUserProvider.getCurrentUserId()
                .filter(this::isUuid)
                .map(UUID::fromString)
                .orElse(null);
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
