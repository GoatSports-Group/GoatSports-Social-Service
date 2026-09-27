package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.ConversationType;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UUID contextId;
    /** Khong bat buoc; controller luon dung nguoi dang dang nhap. */
    private UUID creatorId;
    private List<MemberItemRequest> members;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberItemRequest {
        private UUID userId;
    }
}
