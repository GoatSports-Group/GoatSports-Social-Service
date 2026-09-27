package iuh.fit.socialservice.application.dto.response;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseCommand {
    private UUID postId;
    private UUID authorId;
    private String content;
    private PostVisibility visibility;
    private PostStatus status;
    private PostSport sport;
    /** Bai goc khi day la bai chia se. */
    private UUID sharedPostId;
    /** Bai goc, chi co khi nguoi xem van duoc xem no. */
    private PostResponseCommand sharedPost;
    /** Bai goc da bi xoa, bi an hoac nguoi xem khong co quyen xem. */
    private boolean sharedPostUnavailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private List<AttachmentItem> attachments;
    private List<String> tags;
    private List<UUID> mentions;
    private long likeCount;
    private long commentCount;
    private long shareCount;
    private boolean likedByCurrentUser;
    private boolean savedByCurrentUser;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItem {
        private UUID attachmentId;
        private String storageKey;
        private AttachmentType type;
        private Integer displayOrder;
    }
}
