package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.entity.Mention;
import iuh.fit.socialservice.domain.entity.PlayerCall;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePostCommand {
    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_MENTIONS = 20;

    private UUID actorId;
    private String content;
    private PostVisibility visibility;
    private PostSport sport;
    /** Nguoi duoc nhac ten (@) trong bai. */
    private List<Mention> mentions;
    private List<AttachmentItem> attachments;
    /** Bai "Tim nguoi choi"; null voi bai thuong. */
    private PlayerCall playerCall;
    /** Bai "khoe" dang sua: the da du noi dung nen loi dan duoc de trong. Khong nhan tu client. */
    private boolean highlight;

    public void validate() throws ServiceException {
        if (visibility == null) {
            throw new ServiceException("Quyền riêng tư của bài viết không được để trống");
        }
        if (playerCall != null) validatePlayerCall();
        List<AttachmentItem> submittedAttachments = attachments == null ? List.of() : attachments;
        // Bai tim nguoi choi da co day du thong tin keo, loi nhan them la tuy chon.
        if ((content == null || content.isBlank()) && submittedAttachments.isEmpty() && playerCall == null && !highlight) {
            throw new ServiceException("Bài viết phải có nội dung hoặc tệp đính kèm");
        }
        if (content != null && content.trim().length() > 5000) {
            throw new ServiceException("Nội dung bài viết không được vượt quá 5000 ký tự");
        }
        if (mentions != null && mentions.size() > MAX_MENTIONS) {
            throw new ServiceException("Mỗi bài viết nhắc tối đa " + MAX_MENTIONS + " người");
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

    private void validatePlayerCall() throws ServiceException {
        PlayerCall call = playerCall;
        if (call.sport() == null) throw new ServiceException("Hãy chọn môn cho kèo tìm người chơi");
        if (call.playDate() == null || call.playDate().isBefore(LocalDate.now())) {
            throw new ServiceException("Ngày chơi không được nằm trong quá khứ");
        }
        if (call.startTime() == null || call.endTime() == null || !call.endTime().isAfter(call.startTime())) {
            throw new ServiceException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (call.location() == null || call.location().isBlank() || call.location().length() > 200) {
            throw new ServiceException("Hãy nhập khu vực hoặc sân chơi (tối đa 200 ký tự)");
        }
        if (call.slots() < 1 || call.slots() > PlayerCall.MAX_SLOTS) {
            throw new ServiceException("Số người cần tìm phải từ 1 đến " + PlayerCall.MAX_SLOTS);
        }
        if (call.playFormat() != null && call.playFormat().length() > 40
                || call.skillNote() != null && call.skillNote().length() > 100) {
            throw new ServiceException("Thông tin kèo quá dài");
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
