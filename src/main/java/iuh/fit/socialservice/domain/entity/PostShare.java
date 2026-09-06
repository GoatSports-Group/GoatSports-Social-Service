package iuh.fit.socialservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostShare {
    private UUID shareId;
    private UUID postId;
    private UUID userId;
    private String caption;
    private LocalDateTime sharedAt;
}
