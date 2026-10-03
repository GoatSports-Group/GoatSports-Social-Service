package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.domain.enums.PostSport;

import java.util.UUID;

/** Dich vu noi bo (ai-service, club-service) dang bai "khoe" thay nguoi dung da duoc no xac nhan. */
public record PublishHighlightCommand(UUID authorId, PostSport sport, String content, PostHighlight highlight) {
}
