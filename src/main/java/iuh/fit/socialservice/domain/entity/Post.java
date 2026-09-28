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
    private UUID sharedPostId;
    private PlayerCall playerCall;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    @Builder.Default
    private List<PostAttachment> attachments = new ArrayList<>();
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private List<Mention> mentions = new ArrayList<>();
}
