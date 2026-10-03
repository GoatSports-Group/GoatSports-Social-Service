package iuh.fit.socialservice.presentation.controller;

import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import iuh.fit.socialservice.presentation.response.PageResponse;
import iuh.fit.socialservice.domain.enums.PostSport;
import java.util.Set;
import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialFollowService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialFollowConverter;
import iuh.fit.socialservice.presentation.response.UserFollowStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/follows/users")
@RequiredArgsConstructor
public class SocialUserFollowController {
    private final SocialFollowService followService;
    private final SocialFollowConverter followConverter;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{userId}")
    @ApiMessage("Lay trang thai theo doi thanh cong")
    public ResponseEntity<UserFollowStatusResponse> getStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(followConverter.toResponse(
                followService.getUserFollowStatus(userId, currentUserIdOrNull())
        ));
    }

    @PostMapping("/{userId}")
    @ApiMessage("Theo doi nguoi dung thanh cong")
    public ResponseEntity<UserFollowStatusResponse> followUser(@PathVariable UUID userId)
            throws ServiceException {
        return ResponseEntity.ok(followConverter.toResponse(
                followService.followUser(userId, requiredUserId())
        ));
    }

    @DeleteMapping("/{userId}")
    @ApiMessage("Bo theo doi nguoi dung thanh cong")
    public ResponseEntity<UserFollowStatusResponse> unfollowUser(@PathVariable UUID userId)
            throws ServiceException {
        return ResponseEntity.ok(followConverter.toResponse(
                followService.unfollowUser(userId, requiredUserId())
        ));
    }

    @GetMapping("/suggestions")
    @ApiMessage("Lay goi y theo doi thanh cong")
    public ResponseEntity<List<SocialFollowService.FollowSuggestion>> getSuggestions(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Set<PostSport> sports
    ) throws ServiceException {
        return ResponseEntity.ok(followService.getFollowSuggestions(requiredUserId(), sports, limit, offset));
    }

    @GetMapping("/{userId}/followers")
    @ApiMessage("Lay danh sach nguoi theo doi thanh cong")
    public ResponseEntity<PageResponse<SocialFollowService.FollowEntry>> getFollowers(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) throws ServiceException {
        return ResponseEntity.ok(PageResponse.from(followService.getFollowers(userId, requiredUserId(), pageable)));
    }

    @GetMapping("/{userId}/following")
    @ApiMessage("Lay danh sach dang theo doi thanh cong")
    public ResponseEntity<PageResponse<SocialFollowService.FollowEntry>> getFollowing(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) throws ServiceException {
        return ResponseEntity.ok(PageResponse.from(followService.getFollowing(userId, requiredUserId(), pageable)));
    }

    @GetMapping("/me/following")
    @ApiMessage("Lay danh sach dang theo doi thanh cong")
    public ResponseEntity<List<UUID>> getMyFollowing() throws ServiceException {
        return ResponseEntity.ok(followService.getFollowingUserIds(requiredUserId()));
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
