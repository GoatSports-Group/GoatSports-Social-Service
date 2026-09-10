package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateContentReportCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.service.SocialReportService;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialReportServiceImpl implements SocialReportService {
    private final ContentReportRepository reportRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;

    @Override
    public ContentReportResponseCommand createReport(CreateContentReportCommand command) throws ServiceException {
        CreateContentReportCommand.ValidatedReport validated = command.validate();
        ensureTargetExists(command.getTargetType(), command.getTargetId());
        if (reportRepository.existsOpenReport(
                command.getReporterId(),
                command.getTargetType(),
                command.getTargetId(),
                List.of(ReportStatus.PENDING, ReportStatus.REVIEWING)
        )) {
            throw new ServiceException("Bạn đã báo cáo nội dung này và báo cáo đang được xử lý");
        }

        ContentReport report = ContentReport.builder()
                .reporterId(command.getReporterId())
                .targetType(command.getTargetType())
                .targetId(command.getTargetId())
                .reason(validated.reason())
                .evidence(validated.evidence())
                .status(ReportStatus.PENDING)
                .build();
        return toResponse(reportRepository.save(report));
    }

    @Override
    public Page<ContentReportResponseCommand> getMyReports(UUID reporterId, Pageable pageable)
            throws ServiceException {
        if (reporterId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        return reportRepository.findByReporterId(reporterId, pageable).map(this::toResponse);
    }

    private void ensureTargetExists(ReportTargetType targetType, UUID targetId) throws ServiceException {
        if (targetType == ReportTargetType.POST && postRepository.findById(targetId).isEmpty()) {
            throw new ServiceException("Không tìm thấy bài viết cần báo cáo");
        }
        if (targetType == ReportTargetType.COMMENT && commentRepository.findById(targetId).isEmpty()) {
            throw new ServiceException("Không tìm thấy bình luận cần báo cáo");
        }
        if (targetType != ReportTargetType.POST && targetType != ReportTargetType.COMMENT) {
            throw new ServiceException("Loại nội dung báo cáo chưa được hỗ trợ tại bảng tin");
        }
    }

    private ContentReportResponseCommand toResponse(ContentReport report) {
        return ContentReportResponseCommand.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporterId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .evidence(report.getEvidence())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
