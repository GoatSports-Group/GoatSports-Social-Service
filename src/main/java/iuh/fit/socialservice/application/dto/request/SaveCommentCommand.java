package iuh.fit.socialservice.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import iuh.fit.socialservice.domain.entity.Mention;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveCommentCommand {
    private UUID actorId;
    private UUID parentCommentId;
    private String content;
    private List<Mention> mentions;
}
