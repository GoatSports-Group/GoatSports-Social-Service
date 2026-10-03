package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.PublishHighlightCommand;
import java.util.Set;
import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostCommentResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.Mention;
import iuh.fit.socialservice.domain.enums.PostSport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SocialPostService {
    Page<PostResponseCommand> getFeed(UUID viewerId, FeedFilter filter, Pageable pageable);

    /** Bai da luu cua nguoi xem, moi luu truoc. Bai khong con xem duoc bi bo qua. */
    Page<PostResponseCommand> getSavedPosts(UUID viewerId, Pageable pageable) throws ServiceException;

    /** So bai cua tac gia ma nguoi xem duoc thay, cung quy tac voi bang tin. */
    AuthorStats getAuthorStats(UUID authorId, UUID viewerId);

    PostResponseCommand getPost(UUID postId, UUID viewerId) throws ServiceException;

    PostResponseCommand createPost(SavePostCommand command) throws ServiceException;

    PostResponseCommand updatePost(UUID postId, SavePostCommand command) throws ServiceException;

    void deletePost(UUID postId, UUID actorId) throws ServiceException;

    Page<PostCommentResponseCommand> getComments(UUID postId, UUID viewerId, Pageable pageable)
            throws ServiceException;

    PostCommentResponseCommand createComment(UUID postId, SaveCommentCommand command) throws ServiceException;

    PostCommentResponseCommand updateComment(UUID commentId, UUID actorId, String content, List<Mention> mentions)
            throws ServiceException;

    void deleteComment(UUID commentId, UUID actorId) throws ServiceException;

    PostResponseCommand likePost(UUID postId, UUID actorId) throws ServiceException;

    PostResponseCommand unlikePost(UUID postId, UUID actorId) throws ServiceException;

    PostResponseCommand savePost(UUID postId, UUID actorId) throws ServiceException;

    PostResponseCommand unsavePost(UUID postId, UUID actorId) throws ServiceException;

    /** Tao mot bai chia se tro toi bai goc va tra ve bai moi. */
    PostResponseCommand sharePost(UUID postId, SharePostCommand command) throws ServiceException;

    /** Hashtag dang duoc nhac nhieu trong 7 ngay qua (chi bai cong khai). */
    List<PostRepository.TagCount> getTrendingTags(int limit);

    /** Keo "Tim nguoi choi" con mo, moi dang truoc: {@code club} true = keo CLB, false = keo ca nhan. */
    List<PostResponseCommand> getOpenPlayerCalls(UUID viewerId, boolean club, PostSport sport, int limit);

    /** Nguoi dang danh dau keo da du nguoi (hoac mo lai). */
    PostResponseCommand setPlayerCallFilled(UUID postId, UUID actorId, boolean filled) throws ServiceException;

    /**
     * {@code sports}: cac mon cua nguoi xem ("Mon cua ban"), loc them ben canh {@code sport}.
     * {@code playerCallsOnly}: chi keo con mo (chua du nguoi, chua qua ngay choi), keo sap dien ra truoc.
     */
    /**
     * Dang bai "khoe" do dich vu noi bo da xac nhan (nguoi choi trong tran / chu, quan tri CLB). Luon cong khai,
     * gan the va bao cho tung nguoi duoc gan the.
     */
    PostResponseCommand publishHighlight(PublishHighlightCommand command) throws ServiceException;

    record FeedFilter(boolean followingOnly, PostSport sport, UUID authorId, String tag, boolean playerCallsOnly,
                      Set<PostSport> sports) {
        public FeedFilter(boolean followingOnly, PostSport sport, UUID authorId, String tag, boolean playerCallsOnly) {
            this(followingOnly, sport, authorId, tag, playerCallsOnly, Set.of());
        }

        public FeedFilter(boolean followingOnly, PostSport sport, UUID authorId, String tag) {
            this(followingOnly, sport, authorId, tag, false);
        }
    }

    record AuthorStats(UUID authorId, long postCount) {
    }
}
