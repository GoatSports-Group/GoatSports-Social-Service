package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.request.RespondFriendRequestCommand;
import iuh.fit.socialservice.application.dto.request.SendFriendRequestCommand;
import iuh.fit.socialservice.application.dto.response.FriendshipResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialFriendService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialFriendConverter;
import iuh.fit.socialservice.presentation.request.RespondFriendRequest;
import iuh.fit.socialservice.presentation.request.SendFriendRequest;
import iuh.fit.socialservice.presentation.response.FriendshipResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/social/friends")
@RequiredArgsConstructor
public class SocialFriendController {

    private final SocialFriendService friendService;
    private final SocialFriendConverter friendConverter;

    @PostMapping("/requests")
    @ApiMessage("Gửi lời mời kết bạn thành công")
    public ResponseEntity<FriendshipResponse> sendFriendRequest(@Valid @RequestBody SendFriendRequest request) throws ServiceException {
        SendFriendRequestCommand cmd = friendConverter.toCommand(request);
        FriendshipResponseCommand result = friendService.sendFriendRequest(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(friendConverter.toResponse(result));
    }

    @PutMapping("/requests/{friendshipId}/respond")
    @ApiMessage("Phản hồi lời mời kết bạn thành công")
    public ResponseEntity<FriendshipResponse> respondFriendRequest(
            @PathVariable UUID friendshipId,
            @Valid @RequestBody RespondFriendRequest request
    ) throws ServiceException {
        RespondFriendRequestCommand cmd = friendConverter.toCommand(friendshipId, request);
        FriendshipResponseCommand result = friendService.respondFriendRequest(cmd);
        return ResponseEntity.ok(friendConverter.toResponse(result));
    }

    @DeleteMapping("/{friendshipId}")
    @ApiMessage("Hủy kết bạn thành công")
    public ResponseEntity<Void> unfriend(
            @PathVariable UUID friendshipId,
            @RequestParam UUID userId
    ) throws ServiceException {
        friendService.unfriend(friendshipId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @ApiMessage("Lấy danh sách bạn bè thành công")
    public ResponseEntity<List<FriendshipResponse>> getFriendsList(@RequestParam UUID userId) {
        List<FriendshipResponseCommand> friends = friendService.getFriendsList(userId);
        return ResponseEntity.ok(friends.stream().map(friendConverter::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/requests/received")
    @ApiMessage("Lấy danh sách lời mời kết bạn đã nhận thành công")
    public ResponseEntity<List<FriendshipResponse>> getPendingReceivedRequests(@RequestParam UUID userId) {
        List<FriendshipResponseCommand> requests = friendService.getPendingReceivedRequests(userId);
        return ResponseEntity.ok(requests.stream().map(friendConverter::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/requests/sent")
    @ApiMessage("Lấy danh sách lời mời kết bạn đã gửi thành công")
    public ResponseEntity<List<FriendshipResponse>> getPendingSentRequests(@RequestParam UUID userId) {
        List<FriendshipResponseCommand> requests = friendService.getPendingSentRequests(userId);
        return ResponseEntity.ok(requests.stream().map(friendConverter::toResponse).collect(Collectors.toList()));
    }
}
