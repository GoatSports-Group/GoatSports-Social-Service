package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "post_reactions")
@CompoundIndexes({
        @CompoundIndex(
                name = "reaction_post_user_type_idx",
                def = "{'postId': 1, 'userId': 1, 'type': 1}",
                unique = true
        ),
        @CompoundIndex(name = "reaction_post_time_idx", def = "{'postId': 1, 'reactedAt': -1}"),
        // Danh sach "Da luu" cua mot nguoi, moi nhat truoc.
        @CompoundIndex(name = "reaction_user_type_time_idx", def = "{'userId': 1, 'type': 1, 'reactedAt': -1}")
})
public class PostReactionEntity {
    @Id
    private String reactionId;
    private String postId;
    private String userId;
    private ReactionType type;
    private LocalDateTime reactedAt;
}
