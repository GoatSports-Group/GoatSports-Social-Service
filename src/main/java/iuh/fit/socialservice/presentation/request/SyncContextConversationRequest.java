package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncContextConversationRequest {
    private ConversationType type;

    @NotBlank(message = "Tên cuộc trò chuyện không được để trống")
    private String name;

    private String avatarUrl;

    @NotNull(message = "Chủ sở hữu cuộc trò chuyện không được để trống")
    private UUID ownerId;

    private List<UUID> memberIds;
}
