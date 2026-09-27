package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
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
public class Post {
    private UUID postId;
    private UUID authorId;
    private String content;
    private PostVisibility visibility;
    private PostStatus status;
    private PostSport sport;
    /** Bai goc khi day la bai chia se; null voi bai thuong. */
    private UUID sharedPostId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    @Builder.Default
    private List<PostAttachment> attachments = new ArrayList<>();
}
