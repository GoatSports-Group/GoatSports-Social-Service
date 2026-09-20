package iuh.fit.socialservice.application.dto.request;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.ConversationType;
import lombok.*;

import java.util.List;
import java.util.UUID;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncContextConversationCommand {
    private UUID contextId;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UUID ownerId;
    private List<UUID> memberIds;

    public void validate() throws ServiceException {
        if (contextId == null) throw new ServiceException("Thiếu định danh ngữ cảnh của cuộc trò chuyện.");
        if (ownerId == null) throw new ServiceException("Thiếu chủ sở hữu cuộc trò chuyện.");
        if (name == null || name.isBlank()) throw new ServiceException("Tên cuộc trò chuyện không được để trống.");
    }
}
