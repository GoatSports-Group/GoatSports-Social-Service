package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostSport;
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
    private static final int MAX_ATTACHMENTS = 10;

    private UUID actorId;
    private String content;
    private PostVisibility visibility;
    private PostSport sport;
    private List<AttachmentItem> attachments;

    public void validate() throws ServiceException {
        if (visibility == null) {
            throw new ServiceException("Quyền riêng tư của bài viết không được để trống");
        }
        List<AttachmentItem> submittedAttachments = attachments == null ? List.of() : attachments;
        if ((content == null || content.isBlank()) && submittedAttachments.isEmpty()) {
            throw new ServiceException("Bài viết phải có nội dung hoặc tệp đính kèm");
        }
        if (content != null && content.trim().length() > 5000) {
            throw new ServiceException("Nội dung bài viết không được vượt quá 5000 ký tự");
        }
        if (submittedAttachments.size() > MAX_ATTACHMENTS) {
            throw new ServiceException("Mỗi bài viết được đính kèm tối đa 10 tệp");
        }
        for (AttachmentItem attachment : submittedAttachments) {
            if (attachment == null || attachment.storageKey == null
                    || attachment.storageKey.isBlank() || attachment.type == null) {
                throw new ServiceException("Tệp đính kèm không hợp lệ");
            }
        }
    }

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
