package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.ModerationActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerateContentCommand {
    private UUID reportId;
    private UUID moderatorId;
    private ModerationActionType actionType;
    private String reason;

    public String validate() throws ServiceException {
        if (moderatorId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        if (reportId == null) throw new ServiceException("Báo cáo cần xử lý không hợp lệ");
        if (actionType == null) throw new ServiceException("Hành động kiểm duyệt không hợp lệ");
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.length() < 10 || normalized.length() > 2000) {
            throw new ServiceException("Lý do xử lý phải có từ 10 đến 2000 ký tự");
        }
        return normalized;
    }
}
