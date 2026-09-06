package iuh.fit.socialservice.infrastructure.persistence.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class LegacyConversationDocument {
    @Id
    private String conversationId;
    private String type;
    private String name;
    private String avatarUrl;
    private String createdByUserId;
    private List<LegacyConversationMember> members = new ArrayList<>();
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegacyConversationMember {
        private String memberId;
        private String userId;
        private String userName;
        private String userAvatar;
        private String role;
        private Boolean muted;
        private LocalDateTime joinedAt;
        private LocalDateTime lastReadAt;
    }
}
