package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "friendships",
        indexes = {
                @Index(name = "idx_friendships_requester_status", columnList = "requester_id,status"),
                @Index(name = "idx_friendships_addressee_status", columnList = "addressee_id,status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "friendship_id", columnDefinition = "UUID")
    private UUID friendshipId;

    @Column(name = "requester_id", nullable = false, columnDefinition = "UUID")
    private UUID requesterId;

    @Column(name = "addressee_id", nullable = false, columnDefinition = "UUID")
    private UUID addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FriendshipStatus status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
