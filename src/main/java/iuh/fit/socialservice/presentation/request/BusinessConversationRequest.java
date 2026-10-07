package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.ConversationSubjectType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Nguoi choi bam "Nhan tin" o san / giai / ve: mo chat cong viec voi chu san kem doi tuong dang hoi. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessConversationRequest {
    @NotNull(message = "Thiếu chủ sân / người tổ chức")
    private UUID ownerId;
    @NotNull(message = "Thiếu loại đối tượng")
    private ConversationSubjectType subjectType;
    @NotNull(message = "Thiếu mã đối tượng")
    private UUID subjectId;
}
