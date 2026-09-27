package iuh.fit.socialservice.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Nguoi duoc nhac va dung ten da chen vao noi dung ("@name"). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MentionRequest {
    @NotNull(message = "Người được nhắc không hợp lệ")
    private UUID userId;

    @NotBlank(message = "Tên người được nhắc không được để trống")
    @Size(max = 100, message = "Tên người được nhắc quá dài")
    private String name;
}
