package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.ModerationActionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction extends BaseEntity {
    private UUID actionId;
    private UUID reportId;
    private UUID moderatorId;
    private ModerationActionType actionType;
    private String reason;
    private LocalDateTime actionAt;
}
