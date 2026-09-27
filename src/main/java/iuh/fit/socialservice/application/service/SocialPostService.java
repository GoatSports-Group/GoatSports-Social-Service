package iuh.fit.socialservice.application.service;

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

    record FeedFilter(boolean followingOnly, PostSport sport, UUID authorId, String tag) {
    }

    record AuthorStats(UUID authorId, long postCount) {
    }
}
