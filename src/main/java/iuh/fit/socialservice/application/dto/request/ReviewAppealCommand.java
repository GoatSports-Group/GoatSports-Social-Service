package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.AppealStatus;
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
public class ReviewAppealCommand {
    private UUID appealId;
    private UUID reviewerId;
    private AppealStatus decision;

    public void validate() throws ServiceException {
        if (reviewerId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        if (appealId == null) throw new ServiceException("Khiếu nại cần xử lý không hợp lệ");
        if (decision != AppealStatus.APPROVED && decision != AppealStatus.REJECTED) {
            throw new ServiceException("Quyết định khiếu nại phải là chấp nhận hoặc từ chối");
        }
    }
}
