package iuh.fit.socialservice.presentation.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequest {
    @NotNull(message = "Mã người gửi không được để trống")
    private UUID requesterId;
    @NotNull(message = "Mã người nhận không được để trống")
    private UUID addresseeId;
}
