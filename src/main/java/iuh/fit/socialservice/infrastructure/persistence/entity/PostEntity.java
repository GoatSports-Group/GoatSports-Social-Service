package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "post_feed_idx", def = "{'status': 1, 'visibility': 1, 'publishedAt': -1}"),
        @CompoundIndex(name = "post_author_created_idx", def = "{'authorId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "post_sport_feed_idx", def = "{'status': 1, 'sport': 1, 'publishedAt': -1}"),
        @CompoundIndex(name = "post_tag_feed_idx", def = "{'tags': 1, 'status': 1, 'publishedAt': -1}"),
        @CompoundIndex(name = "post_shared_idx", def = "{'sharedPostId': 1, 'status': 1}")
})
public class PostEntity {
    @Id
    private String postId;

    @Indexed
    private String authorId;
    private String content;
    private PostVisibility visibility;
    private PostStatus status;
    private PostSport sport;
    private String sharedPostId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed
    private LocalDateTime publishedAt;

    @Builder.Default
    private List<PostAttachmentDoc> attachments = new ArrayList<>();
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private List<MentionDoc> mentions = new ArrayList<>();
}
