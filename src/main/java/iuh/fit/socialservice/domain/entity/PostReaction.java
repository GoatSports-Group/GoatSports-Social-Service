package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.ReactionType;
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
public class PostReaction {
    private UUID reactionId;
    private UUID postId;
    private UUID userId;
    private ReactionType type;
    private LocalDateTime reactedAt;
}
