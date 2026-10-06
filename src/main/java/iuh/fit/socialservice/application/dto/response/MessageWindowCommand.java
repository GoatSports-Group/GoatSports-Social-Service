package iuh.fit.socialservice.application.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Mot doan tin quanh tin chua doc dau tien (moi nhat truoc): mo doan chat dung cho nguoi doc dang do.
 * {@code firstUnreadMessageId} null = khong co tin chua doc (doan cuoi cung cua doan chat).
 */
public record MessageWindowCommand(List<MessageResponseCommand> messages, UUID firstUnreadMessageId,
                                   boolean hasOlder, boolean hasNewer) {
}
