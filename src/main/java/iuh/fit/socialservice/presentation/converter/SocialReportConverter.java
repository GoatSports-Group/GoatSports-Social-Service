package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.request.CreateContentReportCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.presentation.request.CreateContentReportRequest;
import iuh.fit.socialservice.presentation.response.ContentReportResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SocialReportConverter {
    public CreateContentReportCommand toCommand(UUID reporterId, CreateContentReportRequest request) {
        return CreateContentReportCommand.builder()
                .reporterId(reporterId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .evidence(request.getEvidence())
                .build();
    }

    public ContentReportResponse toResponse(ContentReportResponseCommand command) {
        return ContentReportResponse.builder()
                .reportId(command.getReportId())
                .reporterId(command.getReporterId())
                .targetType(command.getTargetType())
                .targetId(command.getTargetId())
                .reason(command.getReason())
                .evidence(command.getEvidence())
                .status(command.getStatus())
                .createdAt(command.getCreatedAt())
                .updatedAt(command.getUpdatedAt())
                .build();
    }
}
