package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Hoạt động cộng đồng toàn nền tảng cho dashboard admin. */
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class SocialStatsAdminController {
    private final SocialStatsService socialStatsService;

    @GetMapping("/social")
    @PreAuthorize("hasAuthority('GET_PLATFORM_STATS')")
    public ResponseEntity<SocialStatsService.ActivityStats> social(@RequestParam LocalDate fromDate,
                                                                   @RequestParam LocalDate toDate) throws ServiceException {
        return ResponseEntity.ok(socialStatsService.getActivityStats(fromDate, toDate));
    }
}
