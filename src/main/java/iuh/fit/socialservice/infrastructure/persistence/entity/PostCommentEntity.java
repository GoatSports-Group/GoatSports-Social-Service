package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "post_comments")
@CompoundIndexes({
        @CompoundIndex(name = "comment_post_created_idx", def = "{'postId': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "comment_parent_created_idx", def = "{'parentCommentId': 1, 'createdAt': 1}")
})
public class PostCommentEntity {
    @Id
    private String commentId;

    @Indexed
    private String postId;

    @Indexed
    private String authorId;
    private String parentCommentId;
    private String content;
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
