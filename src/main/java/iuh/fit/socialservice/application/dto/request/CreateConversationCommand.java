package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.domain.enums.ConversationType;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationCommand {
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UUID contextId;
    private UUID creatorId;
    private List<MemberItem> members;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberItem {
        private UUID userId;
    }
}
