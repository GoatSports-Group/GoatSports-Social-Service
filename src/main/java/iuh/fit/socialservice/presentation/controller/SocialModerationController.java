package iuh.fit.socialservice.presentation.controller;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.socialservice.application.dto.request.ModerateContentCommand;
import iuh.fit.socialservice.application.dto.request.ReviewAppealCommand;
import iuh.fit.socialservice.application.dto.response.ModerationTargetResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialModerationService;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialModerationConverter;
import iuh.fit.socialservice.presentation.request.ModerateContentRequest;
import iuh.fit.socialservice.presentation.request.ReviewAppealRequest;
import iuh.fit.socialservice.presentation.response.ContentAppealResponse;
import iuh.fit.socialservice.presentation.response.ContentReportResponse;
import iuh.fit.socialservice.presentation.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/social")
@RequiredArgsConstructor
public class SocialModerationController {
    private final SocialModerationService moderationService;
    private final SocialModerationConverter moderationConverter;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('MODERATE_SOCIAL_CONTENT')")
    @ApiMessage("Lấy hàng đợi báo cáo thành công")
    public ResponseEntity<PageResponse<ContentReportResponse>> getReportQueue(
            @RequestParam(required = false) List<ReportStatus> status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                moderationService.getReportQueue(status, pageable).map(moderationConverter::toResponse)
        ));
    }

    @GetMapping("/reports/{reportId}")
    @PreAuthorize("hasAuthority('MODERATE_SOCIAL_CONTENT')")
    @ApiMessage("Lấy chi tiết báo cáo thành công")
    public ResponseEntity<ContentReportResponse> getReport(@PathVariable UUID reportId) throws ServiceException {
        return ResponseEntity.ok(moderationConverter.toResponse(moderationService.getReport(reportId)));
    }

    @GetMapping("/targets/{targetType}/{targetId}")
    @PreAuthorize("hasAnyAuthority('MODERATE_SOCIAL_CONTENT', 'REVIEW_CONTENT_APPEAL')")
    @ApiMessage("Lấy nội dung bị báo cáo thành công")
    public ResponseEntity<ModerationTargetResponseCommand> getTarget(
            @PathVariable ReportTargetType targetType,
            @PathVariable UUID targetId
    ) {
        return ResponseEntity.ok(moderationService.getTarget(targetType, targetId));
    }

    @PatchMapping("/reports/{reportId}/action")
    @PreAuthorize("hasAuthority('MODERATE_SOCIAL_CONTENT')")
    @AuditLog(action = "MODERATE_SOCIAL_CONTENT", description = "Xử lý báo cáo nội dung mạng xã hội")
    @ApiMessage("Xử lý báo cáo thành công")
    public ResponseEntity<ContentReportResponse> actOnReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ModerateContentRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(moderationConverter.toResponse(
                moderationService.actOnReport(ModerateContentCommand.builder()
                        .reportId(reportId)
                        .moderatorId(requiredUserId())
                        .actionType(request.getActionType())
                        .reason(request.getReason())
                        .build())
        ));
    }

    @GetMapping("/appeals")
    @PreAuthorize("hasAuthority('REVIEW_CONTENT_APPEAL')")
    @ApiMessage("Lấy hàng đợi khiếu nại thành công")
    public ResponseEntity<PageResponse<ContentAppealResponse>> getAppealQueue(
            @RequestParam(required = false) List<AppealStatus> status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                moderationService.getAppealQueue(status, pageable).map(moderationConverter::toResponse)
        ));
    }

    @PatchMapping("/appeals/{appealId}")
    @PreAuthorize("hasAuthority('REVIEW_CONTENT_APPEAL')")
    @AuditLog(action = "REVIEW_CONTENT_APPEAL", description = "Xét duyệt khiếu nại kiểm duyệt nội dung")
    @ApiMessage("Xét duyệt khiếu nại thành công")
    public ResponseEntity<ContentAppealResponse> reviewAppeal(
            @PathVariable UUID appealId,
            @Valid @RequestBody ReviewAppealRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(moderationConverter.toResponse(
                moderationService.reviewAppeal(ReviewAppealCommand.builder()
                        .appealId(appealId)
                        .reviewerId(requiredUserId())
                        .decision(request.getDecision())
                        .build())
        ));
    }

    private UUID requiredUserId() throws ServiceException {
        String value = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Phiên đăng nhập đã hết hạn"));
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Danh tính người dùng không hợp lệ");
        }
    }
}
