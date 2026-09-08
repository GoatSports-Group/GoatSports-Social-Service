package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.ReportTargetType;
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
public class CreateContentReportRequest {
    @NotNull(message = "Loại nội dung báo cáo không được để trống")
    private ReportTargetType targetType;

    @NotNull(message = "Nội dung báo cáo không được để trống")
    private UUID targetId;

    @Size(min = 10, max = 2000, message = "Lý do báo cáo phải có từ 10 đến 2000 ký tự")
    private String reason;

    @Size(max = 5, message = "Tối đa 5 tệp minh chứng")
    @Builder.Default
    private List<@Size(max = 1000, message = "Khóa tệp minh chứng không hợp lệ") String> evidence
            = new ArrayList<>();
}
