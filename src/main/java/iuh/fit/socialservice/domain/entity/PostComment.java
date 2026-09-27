package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostComment {
    private UUID commentId;
    private UUID postId;
    private UUID authorId;
    private UUID parentCommentId;
    private String content;
    @Builder.Default
    private List<Mention> mentions = new ArrayList<>();
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
