package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.enums.HighlightKind;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.PostSport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);

    Optional<Post> findById(UUID postId);

    List<Post> findAllById(Collection<UUID> postIds);

    Page<Post> findFeed(FeedQuery query, Pageable pageable);

    /**
     * Da co bai "khoe" con hien thi cho {@code refId}: giai thi tinh theo CLB ({@code clubId}), tran thi theo nguoi dang.
     */
    boolean existsHighlight(HighlightKind kind, UUID refId, UUID authorId, UUID clubId);

    /**
     * Bai nguoi xem da luu, moi luu truoc. Loc quyen xem va chan truoc khi phan trang, nen moi
     * trang du {@code size} va tong so dung bang so bai con xem duoc.
     */
    Page<Post> findSaved(FeedQuery visibility, Pageable pageable);

    /** Bai goc → so bai chia se dang hien thi tro toi no. */
    Map<UUID, Long> countPublishedShares(Collection<UUID> originalIds);

    /**
     * Tac gia co nhieu bai cong khai nhat tu {@code since}, bo qua {@code excludedAuthorIds};
     * {@code sports} khong rong thi chi dem bai cua cac mon do.
     */
    List<AuthorActivity> findActiveAuthors(LocalDateTime since, Collection<UUID> excludedAuthorIds,
                                           Collection<PostSport> sports, int limit);

    /**
     * Chu de noi bat trong bai cong khai tu {@code since}: nhieu nguoi dung nhat, roi nhieu bai tu {@code risingSince}
     * (dang len), roi tong so bai. Gop theo khoa khong dau; hien cach viet pho bien nhat.
     * {@code sports} khong rong thi chi tinh bai cua cac mon do.
     */
    List<TagCount> findTrendingTags(LocalDateTime since, LocalDateTime risingSince, Collection<PostSport> sports, int limit);

    /**
     * Bo loc bang tin. Quyen xem (cong khai / ban be / cua minh) va danh sach chan luon ap dung;
     * {@code sport}, {@code authorId}, {@code tag} chi thu hep them.
     */
    record FeedQuery(
            UUID viewerId,
            Set<UUID> friendIds,
            Set<UUID> followingIds,
            Set<UUID> blockedUserIds,
            boolean followingOnly,
            PostSport sport,
            UUID authorId,
            String tag,
            PlayerCallFilter playerCalls,
            Set<PostSport> sports
    ) {
        public FeedQuery(UUID viewerId, Set<UUID> friendIds, Set<UUID> followingIds, Set<UUID> blockedUserIds,
                         boolean followingOnly, PostSport sport, UUID authorId, String tag) {
            this(viewerId, friendIds, followingIds, blockedUserIds, followingOnly, sport, authorId, tag, null);
        }

        public FeedQuery(UUID viewerId, Set<UUID> friendIds, Set<UUID> followingIds, Set<UUID> blockedUserIds,
                         boolean followingOnly, PostSport sport, UUID authorId, String tag, PlayerCallFilter playerCalls) {
            this(viewerId, friendIds, followingIds, blockedUserIds, followingOnly, sport, authorId, tag, playerCalls,
                    Set.of());
        }
    }

    /**
     * Chi lay bai "Tim nguoi choi". {@code club}: true = keo CLB, false = keo ca nhan, null = ca hai.
     * {@code openOnly}: bo keo da du nguoi va keo da qua ngay choi.
     */
    record PlayerCallFilter(Boolean club, boolean openOnly) {
    }

    record AuthorActivity(UUID authorId, long postCount) {
    }

    /** @param authorCount so nguoi khac nhau da dung tag (mot nguoi dang nhieu bai van tinh 1) */
    record TagCount(String tag, long postCount, long authorCount) {
    }
}
