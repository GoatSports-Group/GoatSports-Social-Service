package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.MessageType;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    /** Khong bat buoc; controller luon dung nguoi dang dang nhap. */
    private UUID senderId;
    private String clientMessageId;
    private String content;
    private MessageType type;
    private UUID replyToMessageId;
    @Valid
    @Size(max = 10, message = "Mỗi tin nhắn gửi tối đa 10 ảnh")
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
