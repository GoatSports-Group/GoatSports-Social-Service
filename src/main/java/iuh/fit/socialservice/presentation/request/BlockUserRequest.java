package iuh.fit.socialservice.presentation.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockUserRequest {
    @NotNull(message = "Mã người thực hiện chặn không được để trống")
    private UUID blockerId;
    @NotNull(message = "Mã người bị chặn không được để trống")
    private UUID blockedUserId;
    private String reason;
}
