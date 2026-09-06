package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAttachment {
    private UUID attachmentId;
    private String storageKey;
    private AttachmentType type;
    private Integer displayOrder;
}
