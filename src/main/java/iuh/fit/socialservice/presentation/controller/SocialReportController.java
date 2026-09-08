package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialReportService;
import iuh.fit.socialservice.infrastructure.annotation.ApiMessage;
import iuh.fit.socialservice.infrastructure.security.SecurityContextHelper;
import iuh.fit.socialservice.presentation.converter.SocialReportConverter;
import iuh.fit.socialservice.presentation.request.CreateContentReportRequest;
import iuh.fit.socialservice.presentation.response.ContentReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/reports")
@RequiredArgsConstructor
public class SocialReportController {
    private final SocialReportService reportService;
    private final SocialReportConverter reportConverter;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping
    @ApiMessage("Gửi báo cáo nội dung thành công")
    public ResponseEntity<ContentReportResponse> createReport(
            @Valid @RequestBody CreateContentReportRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportConverter.toResponse(
                reportService.createReport(reportConverter.toCommand(requiredUserId(), request))
        ));
    }

    @GetMapping("/me")
    @ApiMessage("Lấy lịch sử báo cáo thành công")
    public ResponseEntity<Page<ContentReportResponse>> getMyReports(
            @PageableDefault(size = 10) Pageable pageable
    ) throws ServiceException {
        return ResponseEntity.ok(reportService.getMyReports(requiredUserId(), pageable)
                .map(reportConverter::toResponse));
    }

    private UUID requiredUserId() throws ServiceException {
        String value = securityContextHelper.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Phiên đăng nhập đã hết hạn"));
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Danh tính người dùng không hợp lệ");
        }
    }
}
