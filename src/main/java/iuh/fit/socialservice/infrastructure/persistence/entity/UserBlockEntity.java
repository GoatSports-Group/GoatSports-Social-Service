package iuh.fit.socialservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_blocks",
        indexes = {
                @Index(name = "idx_user_blocks_blocker", columnList = "blocker_id"),
                @Index(name = "idx_user_blocks_blocked_user", columnList = "blocked_user_id")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlockEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "block_id", columnDefinition = "UUID")
    private UUID blockId;

    @Column(name = "blocker_id", nullable = false, columnDefinition = "UUID")
    private UUID blockerId;

    @Column(name = "blocked_user_id", nullable = false, columnDefinition = "UUID")
    private UUID blockedUserId;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;
}
