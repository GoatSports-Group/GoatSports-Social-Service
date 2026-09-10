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
public class CreateConversationCommand {
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UUID contextId;
    private UUID creatorId;
    private List<MemberItem> members;

    public void validate(int distinctMemberCount) throws ServiceException {
        if (creatorId == null) throw new ServiceException("Thiếu mã người tạo nhóm.");
        if (name == null || name.isBlank()) {
            throw new ServiceException("Tên nhóm trò chuyện không được để trống.");
        }
        int minimumMembers = type == ConversationType.MATCH ? 2 : 3;
        if (distinctMemberCount < minimumMembers) {
            throw new ServiceException(type == ConversationType.MATCH
                    ? "Cuộc trò chuyện trận đấu cần đủ 2 người tham gia."
                    : "Nhóm trò chuyện cần ít nhất 3 thành viên.");
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberItem {
        private UUID userId;
    }
}
