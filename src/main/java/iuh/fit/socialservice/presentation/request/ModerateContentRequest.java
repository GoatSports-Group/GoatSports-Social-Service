package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.ModerationActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerateContentRequest {
    @NotNull(message = "Hanh dong kiem duyet khong duoc de trong")
    private ModerationActionType actionType;

    @Size(min = 10, max = 2000, message = "Ly do xu ly phai co tu 10 den 2000 ky tu")
    private String reason;
}
