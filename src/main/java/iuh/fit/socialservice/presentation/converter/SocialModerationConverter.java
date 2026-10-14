package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.response.ContentAppealResponseCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.presentation.response.ContentAppealResponse;
import iuh.fit.socialservice.presentation.response.ContentReportResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SocialModerationConverter {

    public ContentReportResponse toResponse(ContentReportResponseCommand command) {
        List<ContentReportResponse.ActionItem> actions = command.getActions() == null
                ? List.of()
                : command.getActions().stream()
                        .map(item -> ContentReportResponse.ActionItem.builder()
                                .actionId(item.getActionId())
                                .moderatorId(item.getModeratorId())
                                .actionType(item.getActionType())
                                .reason(item.getReason())
                                .actionAt(item.getActionAt())
                                .build())
                        .toList();
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
                .actions(actions)
                .ai(command.getAi())
                .build();
    }

    public ContentAppealResponse toResponse(ContentAppealResponseCommand command) {
        return ContentAppealResponse.builder()
                .appealId(command.getAppealId())
                .actionId(command.getActionId())
                .appellantId(command.getAppellantId())
                .reason(command.getReason())
                .status(command.getStatus())
                .reviewedBy(command.getReviewedBy())
                .reviewedAt(command.getReviewedAt())
                .createdAt(command.getCreatedAt())
                .actionType(command.getActionType())
                .targetType(command.getTargetType())
                .targetId(command.getTargetId())
                .build();
    }
}
