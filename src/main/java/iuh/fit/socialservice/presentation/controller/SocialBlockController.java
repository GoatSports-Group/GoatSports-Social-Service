package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.request.BlockUserCommand;
import iuh.fit.socialservice.application.dto.response.UserBlockResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.infrastructure.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialBlockConverter;
import iuh.fit.socialservice.presentation.request.BlockUserRequest;
import iuh.fit.socialservice.presentation.response.UserBlockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/social/blocks")
@RequiredArgsConstructor
public class SocialBlockController {

    private final SocialBlockService blockService;
    private final SocialBlockConverter blockConverter;

    @PostMapping
    @ApiMessage("Chặn người dùng thành công")
    public ResponseEntity<UserBlockResponse> blockUser(@Valid @RequestBody BlockUserRequest request) throws ServiceException {
        BlockUserCommand cmd = blockConverter.toCommand(request);
        UserBlockResponseCommand result = blockService.blockUser(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(blockConverter.toResponse(result));
    }

    @DeleteMapping("/{blockId}")
    @ApiMessage("Bỏ chặn người dùng thành công")
    public ResponseEntity<Void> unblockUser(
            @PathVariable UUID blockId,
            @RequestParam UUID actorUserId
    ) throws ServiceException {
        blockService.unblockUser(blockId, actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @ApiMessage("Lấy danh sách người dùng bị chặn thành công")
    public ResponseEntity<List<UserBlockResponse>> getBlockedUsers(@RequestParam UUID userId) {
        List<UserBlockResponseCommand> blocks = blockService.getBlockedUsers(userId);
        return ResponseEntity.ok(blocks.stream().map(blockConverter::toResponse).collect(Collectors.toList()));
    }
}
