package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.MessageType;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotNull(message = "Mã người gửi không được để trống")
    private UUID senderId;
    private String clientMessageId;
    private String content;
    private MessageType type;
    private UUID replyToMessageId;
    private List<AttachmentItemRequest> attachments;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItemRequest {
        private UUID attachmentId;
        @NotBlank(message = "Khóa tệp đính kèm không được để trống")
        private String storageKey;
        @NotNull(message = "Loại tệp đính kèm không được để trống")
        private AttachmentType type;
        private String fileName;
        private Long fileSize;
    }
}
