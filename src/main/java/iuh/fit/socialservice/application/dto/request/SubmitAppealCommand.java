package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
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
public class SubmitAppealCommand {
    private UUID actionId;
    private UUID appellantId;
    private String reason;

    public String validate() throws ServiceException {
        if (appellantId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        if (actionId == null) throw new ServiceException("Quyết định kiểm duyệt không hợp lệ");
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.length() < 10 || normalized.length() > 2000) {
            throw new ServiceException("Lý do khiếu nại phải có từ 10 đến 2000 ký tự");
        }
        return normalized;
    }
}
