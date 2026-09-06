package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {
    private UUID attachmentId;
    private String storageKey;
    private AttachmentType type;
    private String fileName;
    private Long fileSize;
}
