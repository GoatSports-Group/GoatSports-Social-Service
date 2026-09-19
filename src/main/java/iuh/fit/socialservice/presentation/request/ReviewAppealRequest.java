package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.AppealStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAppealRequest {
    @NotNull(message = "Quyet dinh khieu nai khong duoc de trong")
    private AppealStatus decision;
}
