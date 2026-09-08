package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.CreateContentReportCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SocialReportService {
    ContentReportResponseCommand createReport(CreateContentReportCommand command) throws ServiceException;

    Page<ContentReportResponseCommand> getMyReports(UUID reporterId, Pageable pageable) throws ServiceException;
}
