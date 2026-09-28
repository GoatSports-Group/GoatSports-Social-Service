package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Valid
    @Size(max = 20, message = "Mỗi bài viết nhắc tối đa 20 người")
    @Builder.Default
    private List<MentionRequest> mentions = new ArrayList<>();

    @Valid
    @Size(max = 10, message = "Mỗi bài viết được đính kèm tối đa 10 tệp")
    @Builder.Default
    private List<AttachmentItem> attachments = new ArrayList<>();

    /** Tuy chon: bai "Tim nguoi choi" cho keo dang thieu nguoi. */
    @Valid
    private PlayerCallRequest playerCall;

    public record PlayerCallRequest(
            @NotNull(message = "Hãy chọn môn") PostSport sport,
            @Size(max = 40) String playFormat,
            @NotNull(message = "Hãy chọn ngày chơi") LocalDate playDate,
            @NotNull(message = "Hãy chọn giờ bắt đầu") LocalTime startTime,
            @NotNull(message = "Hãy chọn giờ kết thúc") LocalTime endTime,
            @NotBlank(message = "Hãy nhập khu vực hoặc sân chơi") @Size(max = 200) String location,
            @Min(1) @Max(30) int slots,
            @Size(max = 100) String skillNote,
            /** Chi chu hoac quan ly CLB moi dang thay CLB duoc (kiem tra o service). */
            UUID clubId
    ) { }

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
