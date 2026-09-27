package iuh.fit.socialservice.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import iuh.fit.socialservice.domain.enums.PostVisibility;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharePostCommand {
    private UUID actorId;
    private String caption;
    private PostVisibility visibility;
}
