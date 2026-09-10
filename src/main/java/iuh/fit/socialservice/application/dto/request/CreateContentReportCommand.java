package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
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
public class CreateContentReportCommand {
    private UUID reporterId;
    private ReportTargetType targetType;
    private UUID targetId;
    private String reason;
    private List<String> evidence;

    public ValidatedReport validate() throws ServiceException {
        if (reporterId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        if (targetType == null || targetId == null) {
            throw new ServiceException("Nội dung cần báo cáo không hợp lệ");
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < 10 || normalizedReason.length() > 2000) {
            throw new ServiceException("Lý do báo cáo phải có từ 10 đến 2000 ký tự");
        }
        List<String> normalizedEvidence = evidence == null ? List.of() : evidence.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedEvidence.size() > 5
                || normalizedEvidence.stream().anyMatch(value -> value.length() > 1000)) {
            throw new ServiceException("Tối đa 5 tệp minh chứng hợp lệ");
        }
        return new ValidatedReport(normalizedReason, normalizedEvidence);
    }

    public record ValidatedReport(String reason, List<String> evidence) {
    }
}
