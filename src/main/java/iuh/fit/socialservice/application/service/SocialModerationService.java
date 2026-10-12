package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.ModerateContentCommand;
import iuh.fit.socialservice.application.dto.request.ReviewAppealCommand;
import iuh.fit.socialservice.application.dto.request.SubmitAppealCommand;
import iuh.fit.socialservice.application.dto.response.ContentAppealResponseCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.UUID;

public interface SocialModerationService {
    Page<ContentReportResponseCommand> getReportQueue(Collection<ReportStatus> statuses, Pageable pageable);

    ContentReportResponseCommand getReport(UUID reportId) throws ServiceException;

    /** Nội dung đang bị báo cáo / khiếu nại (bài viết, bình luận, tin nhắn), để admin đọc đầy đủ trước khi xử lý. */
    iuh.fit.socialservice.application.dto.response.ModerationTargetResponseCommand getTarget(
            iuh.fit.socialservice.domain.enums.ReportTargetType targetType, UUID targetId);

    ContentReportResponseCommand actOnReport(ModerateContentCommand command) throws ServiceException;

    ContentAppealResponseCommand submitAppeal(SubmitAppealCommand command) throws ServiceException;

    ContentAppealResponseCommand reviewAppeal(ReviewAppealCommand command) throws ServiceException;

    Page<ContentAppealResponseCommand> getAppealQueue(Collection<AppealStatus> statuses, Pageable pageable);

    Page<ContentAppealResponseCommand> getMyAppeals(UUID appellantId, Pageable pageable) throws ServiceException;
}
