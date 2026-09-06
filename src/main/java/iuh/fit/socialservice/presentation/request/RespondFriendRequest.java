package iuh.fit.socialservice.presentation.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondFriendRequest {
    @NotNull(message = "Mã người phản hồi không được để trống")
    private UUID actorUserId;
    private boolean accepted;
}
