package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostCommentResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostShareResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.application.service.SocialPostService;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.application.port.external.StorageKeyPromoter;
import iuh.fit.socialservice.domain.entity.PostAttachment;
import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.entity.PostShare;
import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.domain.enums.ReactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialPostServiceImpl implements SocialPostService {
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final PostShareRepository shareRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final FollowRepository followRepository;
    private final StorageKeyPromoter storageKeyPromoter;

    @Override
    public Page<PostResponseCommand> getFeed(UUID viewerId, boolean followingOnly, Pageable pageable) {
        Set<UUID> friendIds = viewerId == null ? Set.of() : friendIds(viewerId);
        Set<UUID> blockedIds = viewerId == null ? Set.of() : blockedUserIds(viewerId);
        Set<UUID> followingIds = viewerId == null || !followingOnly ? Set.of() : followingUserIds(viewerId);
        return postRepository.findFeed(viewerId, friendIds, followingIds, blockedIds, followingOnly, pageable)
                .map(post -> toResponse(post, viewerId));
    }

    @Override
    public PostResponseCommand getPost(UUID postId, UUID viewerId) throws ServiceException {
        Post post = publishedPost(postId);
        ensureVisible(post, viewerId);
        return toResponse(post, viewerId);
    }

    @Override
    public PostResponseCommand createPost(SavePostCommand command) throws ServiceException {
        requireActor(command.getActorId());
        command.validate();
        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder()
                .authorId(command.getActorId())
                .content(normalizeContent(command.getContent()))
                .visibility(command.getVisibility())
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .attachments(toAttachments(command.getAttachments()))
                .build();
        return toResponse(postRepository.save(post), command.getActorId());
    }

    @Override
    public PostResponseCommand updatePost(UUID postId, SavePostCommand command) throws ServiceException {
        requireActor(command.getActorId());
        command.validate();
        Post post = ownedPost(postId, command.getActorId());
        if (post.getStatus() == PostStatus.REMOVED) {
            throw new ServiceException("Bài viết đã bị xóa");
        }
        post.setContent(normalizeContent(command.getContent()));
        post.setVisibility(command.getVisibility());
        post.setAttachments(toAttachments(command.getAttachments()));
        post.setUpdatedAt(LocalDateTime.now());
        return toResponse(postRepository.save(post), command.getActorId());
    }

    @Override
    public void deletePost(UUID postId, UUID actorId) throws ServiceException {
        Post post = ownedPost(postId, requireActor(actorId));
        post.setStatus(PostStatus.REMOVED);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    public Page<PostCommentResponseCommand> getComments(
            UUID postId,
            UUID viewerId,
            Pageable pageable
    ) throws ServiceException {
        Post post = publishedPost(postId);
        ensureVisible(post, viewerId);
        return commentRepository.findPublishedByPostId(postId, pageable).map(this::toResponse);
    }

    @Override
    public PostCommentResponseCommand createComment(UUID postId, SaveCommentCommand command)
            throws ServiceException {
        requireActor(command.getActorId());
        Post post = publishedPost(postId);
        ensureVisible(post, command.getActorId());
        String content = requiredText(command.getContent(), "Nội dung bình luận không được để trống", 1000);
        if (command.getParentCommentId() != null) {
            PostComment parent = commentRepository.findById(command.getParentCommentId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy bình luận được trả lời"));
            if (!postId.equals(parent.getPostId()) || parent.getStatus() != PostStatus.PUBLISHED) {
                throw new ServiceException("Bình luận được trả lời không thuộc bài viết hiện tại");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        PostComment comment = PostComment.builder()
                .postId(postId)
                .authorId(command.getActorId())
                .parentCommentId(command.getParentCommentId())
                .content(content)
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public PostCommentResponseCommand updateComment(UUID commentId, UUID actorId, String content)
            throws ServiceException {
        PostComment comment = ownedComment(commentId, requireActor(actorId));
        if (comment.getStatus() != PostStatus.PUBLISHED) {
            throw new ServiceException("Bình luận không còn khả dụng");
        }
        comment.setContent(requiredText(content, "Nội dung bình luận không được để trống", 1000));
        comment.setUpdatedAt(LocalDateTime.now());
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(UUID commentId, UUID actorId) throws ServiceException {
        PostComment comment = ownedComment(commentId, requireActor(actorId));
        comment.setStatus(PostStatus.REMOVED);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Override
    public PostResponseCommand likePost(UUID postId, UUID actorId) throws ServiceException {
        UUID userId = requireActor(actorId);
        Post post = publishedPost(postId);
        ensureVisible(post, userId);
        if (reactionRepository.find(postId, userId, ReactionType.LIKE).isEmpty()) {
            reactionRepository.save(PostReaction.builder()
                    .postId(postId)
                    .userId(userId)
                    .type(ReactionType.LIKE)
                    .reactedAt(LocalDateTime.now())
                    .build());
        }
        return toResponse(post, userId);
    }

    @Override
    public PostResponseCommand unlikePost(UUID postId, UUID actorId) throws ServiceException {
        UUID userId = requireActor(actorId);
        Post post = publishedPost(postId);
        ensureVisible(post, userId);
        reactionRepository.delete(postId, userId, ReactionType.LIKE);
        return toResponse(post, userId);
    }

    @Override
    public PostShareResponseCommand sharePost(UUID postId, SharePostCommand command) throws ServiceException {
        UUID actorId = requireActor(command.getActorId());
        Post post = publishedPost(postId);
        ensureVisible(post, actorId);
        String caption = command.getCaption() == null || command.getCaption().isBlank()
                ? null
                : requiredText(command.getCaption(), "Chú thích không hợp lệ", 1000);
        PostShare share = shareRepository.save(PostShare.builder()
                .postId(postId)
                .userId(actorId)
                .caption(caption)
                .sharedAt(LocalDateTime.now())
                .build());
        return PostShareResponseCommand.builder()
                .shareId(share.getShareId())
                .postId(share.getPostId())
                .userId(share.getUserId())
                .caption(share.getCaption())
                .sharedAt(share.getSharedAt())
                .build();
    }

    private List<PostAttachment> toAttachments(List<SavePostCommand.AttachmentItem> items) {
        if (items == null) return List.of();
        List<SavePostCommand.AttachmentItem> ordered = items.stream()
                .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? 0 : item.getDisplayOrder()))
                .toList();

        // Anh vua upload nam o `temp/`; doi sang vung vinh vien truoc khi luu khoa vao bai viet,
        // neu khong thi anh chon roi khong dang se tich lai mai va lifecycle rule tren `temp/`
        // se xoa mat anh cua bai da dang.
        List<String> promoted = storageKeyPromoter.promote(
                ordered.stream().map(item -> item.getStorageKey().trim()).toList(),
                "social-post");

        List<PostAttachment> attachments = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            SavePostCommand.AttachmentItem item = ordered.get(index);
            attachments.add(PostAttachment.builder()
                    .storageKey(promoted.get(index))
                    .type(item.getType())
                    .displayOrder(item.getDisplayOrder() == null ? 0 : item.getDisplayOrder())
                    .build());
        }
        return attachments;
    }

    private Post publishedPost(UUID postId) throws ServiceException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết"));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ServiceException("Bài viết không còn khả dụng");
        }
        return post;
    }

    private Post ownedPost(UUID postId, UUID actorId) throws ServiceException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết"));
        if (!actorId.equals(post.getAuthorId())) {
            throw new ServiceException("Bạn không có quyền chỉnh sửa bài viết này");
        }
        return post;
    }

    private PostComment ownedComment(UUID commentId, UUID actorId) throws ServiceException {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bình luận"));
        if (!actorId.equals(comment.getAuthorId())) {
            throw new ServiceException("Bạn không có quyền chỉnh sửa bình luận này");
        }
        return comment;
    }

    private void ensureVisible(Post post, UUID viewerId) throws ServiceException {
        if (post.getVisibility() == PostVisibility.PUBLIC) return;
        if (viewerId == null) throw new ServiceException("Bạn cần đăng nhập để xem bài viết này");
        if (viewerId.equals(post.getAuthorId())) return;
        if (blockedUserIds(viewerId).contains(post.getAuthorId())) {
            throw new ServiceException("Bài viết không khả dụng");
        }
        if (post.getVisibility() == PostVisibility.FRIENDS
                && friendIds(viewerId).contains(post.getAuthorId())) return;
        throw new ServiceException("Bạn không có quyền xem bài viết này");
    }

    private Set<UUID> friendIds(UUID userId) {
        Set<UUID> result = new HashSet<>();
        for (Friendship friendship : friendshipRepository.findUserFriends(userId, FriendshipStatus.ACCEPTED)) {
            result.add(userId.equals(friendship.getRequesterId())
                    ? friendship.getAddresseeId()
                    : friendship.getRequesterId());
        }
        return result;
    }

    private Set<UUID> followingUserIds(UUID userId) {
        Set<UUID> result = new HashSet<>();
        for (Follow follow : followRepository.findByFollowerIdAndTargetType(userId, FollowTargetType.USER)) {
            result.add(follow.getTargetId());
        }
        return result;
    }

    private Set<UUID> blockedUserIds(UUID userId) {
        Set<UUID> result = new HashSet<>();
        for (UserBlock block : userBlockRepository.findRelatedToUser(userId)) {
            result.add(userId.equals(block.getBlockerId()) ? block.getBlockedUserId() : block.getBlockerId());
        }
        return result;
    }

    private PostResponseCommand toResponse(Post post, UUID viewerId) {
        return PostResponseCommand.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .attachments(post.getAttachments().stream()
                        .sorted(Comparator.comparing(
                                attachment -> attachment.getDisplayOrder() == null ? 0 : attachment.getDisplayOrder()
                        ))
                        .map(attachment -> PostResponseCommand.AttachmentItem.builder()
                                .attachmentId(attachment.getAttachmentId())
                                .storageKey(attachment.getStorageKey())
                                .type(attachment.getType())
                                .displayOrder(attachment.getDisplayOrder())
                                .build())
                        .toList())
                .likeCount(reactionRepository.count(post.getPostId(), ReactionType.LIKE))
                .commentCount(commentRepository.countPublishedByPostId(post.getPostId()))
                .shareCount(shareRepository.countByPostId(post.getPostId()))
                .likedByCurrentUser(viewerId != null
                        && reactionRepository.find(post.getPostId(), viewerId, ReactionType.LIKE).isPresent())
                .build();
    }

    private PostCommentResponseCommand toResponse(PostComment comment) {
        return PostCommentResponseCommand.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private UUID requireActor(UUID actorId) throws ServiceException {
        if (actorId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        return actorId;
    }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.trim();
    }

    private String requiredText(String value, String emptyMessage, int maxLength) throws ServiceException {
        if (value == null || value.isBlank()) throw new ServiceException(emptyMessage);
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ServiceException("Nội dung không được vượt quá " + maxLength + " ký tự");
        }
        return normalized;
    }
}
