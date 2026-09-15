package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.dto.response.PresenceStatusCommand;
import iuh.fit.socialservice.application.service.SocialPresenceService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/presence")
@RequiredArgsConstructor
public class SocialPresenceController {
    private final SocialPresenceService presenceService;

    @GetMapping
    @ApiMessage("Lấy trạng thái hoạt động thành công")
    public ResponseEntity<List<PresenceStatusCommand>> getPresence(@RequestParam Set<UUID> userIds) {
        return ResponseEntity.ok(presenceService.getPresence(userIds));
    }
}
