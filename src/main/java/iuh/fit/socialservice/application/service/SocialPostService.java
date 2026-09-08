package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostCommentResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostShareResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SocialPostService {
    Page<PostResponseCommand> getFeed(UUID viewerId, Pageable pageable);

    PostResponseCommand getPost(UUID postId, UUID viewerId) throws ServiceException;

    PostResponseCommand createPost(SavePostCommand command) throws ServiceException;

    PostResponseCommand updatePost(UUID postId, SavePostCommand command) throws ServiceException;

    void deletePost(UUID postId, UUID actorId) throws ServiceException;

    Page<PostCommentResponseCommand> getComments(UUID postId, UUID viewerId, Pageable pageable)
            throws ServiceException;

    PostCommentResponseCommand createComment(UUID postId, SaveCommentCommand command) throws ServiceException;

    PostCommentResponseCommand updateComment(UUID commentId, UUID actorId, String content)
            throws ServiceException;

    void deleteComment(UUID commentId, UUID actorId) throws ServiceException;

    PostResponseCommand likePost(UUID postId, UUID actorId) throws ServiceException;

    PostResponseCommand unlikePost(UUID postId, UUID actorId) throws ServiceException;

    PostShareResponseCommand sharePost(UUID postId, SharePostCommand command) throws ServiceException;
}
