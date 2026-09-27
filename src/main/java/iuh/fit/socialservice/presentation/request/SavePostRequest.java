package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePostRequest {
    @Size(max = 5000, message = "Nội dung bài viết không được vượt quá 5000 ký tự")
    private String content;

    @NotNull(message = "Quyền riêng tư không được để trống")
    private PostVisibility visibility;

    /** Tuy chon; null nghia la bai khong gan mon. */
    private PostSport sport;

    @Size(max = 20, message = "Mỗi bài viết nhắc tối đa 20 người")
    @Builder.Default
    private List<UUID> mentions = new ArrayList<>();

    @Valid
    @Size(max = 10, message = "Mỗi bài viết được đính kèm tối đa 10 tệp")
    @Builder.Default
    private List<AttachmentItem> attachments = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItem {
        @NotBlank(message = "Khóa tệp đính kèm không được để trống")
        private String storageKey;

        @NotNull(message = "Loại tệp đính kèm không được để trống")
        private AttachmentType type;

        private Integer displayOrder;
    }
}
