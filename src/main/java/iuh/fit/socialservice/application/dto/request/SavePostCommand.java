package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePostCommand {
    private UUID actorId;
    private String content;
    private PostVisibility visibility;
    private List<AttachmentItem> attachments;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItem {
        private String storageKey;
        private AttachmentType type;
        private Integer displayOrder;
    }
}
