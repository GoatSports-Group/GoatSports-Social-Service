package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.PostVisibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharePostRequest {
    @Size(max = 1000, message = "Chú thích không được vượt quá 1000 ký tự")
    private String caption;

    /** Mac dinh PUBLIC. */
    private PostVisibility visibility;
}
