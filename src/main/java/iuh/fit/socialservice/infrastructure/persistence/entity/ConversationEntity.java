package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.ConversationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "conversations",
        indexes = @Index(name = "idx_conversations_last_message_at", columnList = "last_message_at")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "conversation_id", columnDefinition = "UUID")
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ConversationType type;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_id", columnDefinition = "UUID")
    private UUID lastMessageId;

    @Column(name = "context_id", columnDefinition = "UUID")
    private UUID contextId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", length = 32)
    private iuh.fit.socialservice.domain.enums.ConversationSubjectType subjectType;

    @Column(name = "subject_id", columnDefinition = "UUID")
    private UUID subjectId;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("joinedAt ASC")
    @Builder.Default
    private List<ConversationMemberEntity> members = new ArrayList<>();

    public void replaceMembers(List<ConversationMemberEntity> newMembers) {
        members.clear();
        if (newMembers == null) return;
        newMembers.forEach(member -> {
            member.setConversation(this);
            members.add(member);
        });
    }
}
