package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.AppealStatus;
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
public class ContentAppeal extends BaseEntity {
    private UUID appealId;
    private UUID actionId;
    private UUID appellantId;
    private String reason;
    private AppealStatus status;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
}
