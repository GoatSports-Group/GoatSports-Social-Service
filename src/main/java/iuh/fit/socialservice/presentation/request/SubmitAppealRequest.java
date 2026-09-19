package iuh.fit.socialservice.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class SubmitAppealRequest {
    @NotNull(message = "Quyet dinh kiem duyet khong duoc de trong")
    private UUID actionId;

    @Size(min = 10, max = 2000, message = "Ly do khieu nai phai co tu 10 den 2000 ky tu")
    private String reason;
}
