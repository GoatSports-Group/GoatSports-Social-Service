package iuh.fit.socialservice.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlock extends BaseEntity {
    private UUID blockId;
    private UUID blockerId;
    private UUID blockedUserId;
    private String reason;
    private LocalDateTime blockedAt;
}
