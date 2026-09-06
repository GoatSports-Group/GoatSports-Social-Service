package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReceipt {
    private UUID receiptId;
    private UUID userId;
    private MessageStatus status;
    private LocalDateTime occurredAt;
}
