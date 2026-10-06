package iuh.fit.socialservice.presentation.response;

import java.util.List;
import java.util.UUID;

public record MessageWindowResponse(List<MessageResponse> messages, UUID firstUnreadMessageId,
                                    boolean hasOlder, boolean hasNewer) {
}
