package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostCommentResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
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
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final SocialEventPublisher eventPublisher;

    @Override
    public Page<PostResponseCommand> getFeed(UUID viewerId, FeedFilter filter, Pageable pageable) {
        return postRepository.findFeed(feedQuery(viewerId, filter), pageable)
                .map(post -> toResponse(post, viewerId));
    }

    @Override
    public Page<PostResponseCommand> getSavedPosts(UUID viewerId, Pageable pageable) throws ServiceException {
        UUID userId = requireActor(viewerId);
        Page<PostReaction> saves = reactionRepository.findByUser(userId, ReactionType.SAVE, pageable);
        Map<UUID, Post> posts = postRepository.findAllById(saves.map(PostReaction::getPostId).getContent())
                .stream()
                .collect(Collectors.toMap(Post::getPostId, Function.identity()));
        // ponytail: bai bi an / doi quyen rieng tu duoc loc sau khi phan trang nen mot trang co the ngan hon
        // `size`; chuyen sang $lookup neu so bai luu bi an tro nen dang ke.
        List<PostResponseCommand> content = saves.getContent().stream()
                .map(save -> posts.get(save.getPostId()))
                .filter(Objects::nonNull)
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED && canView(post, userId))
                .map(post -> toResponse(post, userId))
                .toList();
        return new PageImpl<>(content, pageable, saves.getTotalElements());
    }

    @Override
    public AuthorStats getAuthorStats(UUID authorId, UUID viewerId) {
        long total = postRepository
                .findFeed(feedQuery(viewerId, new FeedFilter(false, null, authorId)), PageRequest.of(0, 1))
                .getTotalElements();
        return new AuthorStats(authorId, total);
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
                .sport(command.getSport())
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
        Post post = ownedPost(postId, command.getActorId());
        if (post.getStatus() == PostStatus.REMOVED) {
            throw new ServiceException("Bài viết đã bị xóa");
        }
        if (post.getSharedPostId() != null) {
            // Bai chia se chi sua duoc loi dan va quyen rieng tu; noi dung chinh thuoc bai goc.
            if (command.getVisibility() == null) {
                throw new ServiceException("Quyền riêng tư của bài viết không được để trống");
            }
            post.setContent(command.getContent() == null || command.getContent().isBlank()
                    ? null
                    : requiredText(command.getContent(), "Chú thích không hợp lệ", 1000));
            post.setVisibility(command.getVisibility());
            post.setUpdatedAt(LocalDateTime.now());
            return toResponse(postRepository.save(post), command.getActorId());
        }
        command.validate();
        post.setContent(normalizeContent(command.getContent()));
        post.setVisibility(command.getVisibility());
        post.setSport(command.getSport());
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
        PostComment parent = null;
        if (command.getParentCommentId() != null) {
            parent = commentRepository.findById(command.getParentCommentId())
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
        PostComment saved = commentRepository.save(comment);
        notifyComment(post, parent, saved);
        return toResponse(saved);
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
        if (addReaction(postId, userId, ReactionType.LIKE)) {
            // ponytail: bo thich roi thich lai se bao lai; gom theo (bai, nguoi) neu bi phan nan spam.
            notify(SocialEventPublisher.POST_LIKED, post.getAuthorId(), userId, post, snippet(post.getContent()));
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
    public PostResponseCommand savePost(UUID postId, UUID actorId) throws ServiceException {
        UUID userId = requireActor(actorId);
        Post post = publishedPost(postId);
        ensureVisible(post, userId);
        addReaction(postId, userId, ReactionType.SAVE);
        return toResponse(post, userId);
    }

    @Override
    public PostResponseCommand unsavePost(UUID postId, UUID actorId) throws ServiceException {
        UUID userId = requireActor(actorId);
        // Bo luu luon duoc phep, ke ca khi bai da bi an, de nguoi dung don duoc danh sach cua minh.
        reactionRepository.delete(postId, userId, ReactionType.SAVE);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết"));
        return toResponse(post, userId);
    }

    @Override
    public PostResponseCommand sharePost(UUID postId, SharePostCommand command) throws ServiceException {
        UUID actorId = requireActor(command.getActorId());
        Post target = publishedPost(postId);
        ensureVisible(target, actorId);
        // Chia se lai mot bai chia se thi tro thang ve bai goc, khong tao chuoi long nhau.
        Post original = target.getSharedPostId() == null ? target : publishedPost(target.getSharedPostId());
        ensureVisible(original, actorId);
        if (original.getVisibility() != PostVisibility.PUBLIC) {
            throw new ServiceException("Chỉ có thể chia sẻ bài viết công khai");
        }
        String caption = command.getCaption() == null || command.getCaption().isBlank()
                ? null
                : requiredText(command.getCaption(), "Chú thích không hợp lệ", 1000);
        LocalDateTime now = LocalDateTime.now();
        Post share = postRepository.save(Post.builder()
                .authorId(actorId)
                .content(caption)
                .visibility(command.getVisibility() == null ? PostVisibility.PUBLIC : command.getVisibility())
                .sport(original.getSport())
                .sharedPostId(original.getPostId())
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .build());
        shareRepository.save(PostShare.builder()
                .postId(original.getPostId())
                .userId(actorId)
                .caption(caption)
                .sharedAt(now)
                .build());
        notify(SocialEventPublisher.POST_SHARED, original.getAuthorId(), actorId, original, snippet(caption));
        return toResponse(share, actorId);
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
        if (canView(post, viewerId)) return;
        if (viewerId == null) throw new UnauthenticatedException("Bạn cần đăng nhập để xem bài viết này");
        throw new ServiceException("Bạn không có quyền xem bài viết này");
    }

    /**
     * Chan hai chieu ap dung ca voi bai cong khai: neu khong, nguoi bi chan van thich / binh luan
     * duoc bai cong khai cua nguoi da chan minh bang cach goi thang theo id.
     */
    private boolean canView(Post post, UUID viewerId) {
        if (viewerId != null && viewerId.equals(post.getAuthorId())) return true;
        if (viewerId != null && blockedUserIds(viewerId).contains(post.getAuthorId())) return false;
        if (post.getVisibility() == PostVisibility.PUBLIC) return true;
        if (viewerId == null) return false;
        return post.getVisibility() == PostVisibility.FRIENDS && friendIds(viewerId).contains(post.getAuthorId());
    }

    private PostRepository.FeedQuery feedQuery(UUID viewerId, FeedFilter filter) {
        boolean followingOnly = filter.followingOnly() && viewerId != null;
        return new PostRepository.FeedQuery(
                viewerId,
                viewerId == null ? Set.of() : friendIds(viewerId),
                followingOnly ? followingUserIds(viewerId) : Set.of(),
                viewerId == null ? Set.of() : blockedUserIds(viewerId),
                followingOnly,
                filter.sport(),
                filter.authorId());
    }

    /** true khi vua tao moi (truoc do chua co). */
    private boolean addReaction(UUID postId, UUID userId, ReactionType type) {
        if (reactionRepository.find(postId, userId, type).isPresent()) return false;
        reactionRepository.save(PostReaction.builder()
                .postId(postId)
                .userId(userId)
                .type(type)
                .reactedAt(LocalDateTime.now())
                .build());
        return true;
    }

    private void notifyComment(Post post, PostComment parent, PostComment comment) {
        UUID actorId = comment.getAuthorId();
        String snippet = snippet(comment.getContent());
        notify(SocialEventPublisher.POST_COMMENTED, post.getAuthorId(), actorId, post, snippet);
        if (parent != null && !parent.getAuthorId().equals(post.getAuthorId())
                && !blockedUserIds(actorId).contains(parent.getAuthorId())) {
            notify(SocialEventPublisher.COMMENT_REPLIED, parent.getAuthorId(), actorId, post, snippet);
        }
    }

    /** Khong bao cho chinh minh. Quan he chan voi tac gia bai da bi canView loai truoc khi toi day. */
    private void notify(String eventType, UUID receiverId, UUID actorId, Post post, String snippet) {
        if (receiverId == null || receiverId.equals(actorId)) return;
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("postId", post.getPostId().toString());
        payload.put("snippet", snippet);
        eventPublisher.publish(eventType, receiverId, actorId, post.getPostId(), payload);
    }

    static String snippet(String text) {
        if (text == null || text.isBlank()) return "";
        String flat = text.strip().replaceAll("\\s+", " ");
        return flat.length() <= 80 ? flat : flat.substring(0, 79) + "\u2026";
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
        PostResponseCommand response = baseResponse(post, viewerId);
        if (post.getSharedPostId() != null) {
            // Bai goc duoc kiem quyen lai cho tung nguoi xem, nen chia se khong lam lo bai da bi an.
            Post original = postRepository.findById(post.getSharedPostId())
                    .filter(item -> item.getStatus() == PostStatus.PUBLISHED && canView(item, viewerId))
                    .orElse(null);
            response.setSharedPost(original == null ? null : baseResponse(original, viewerId));
            response.setSharedPostUnavailable(original == null);
        }
        return response;
    }

    // ponytail: vai truy van dem cho moi bai (10 bai/trang); gom theo lo bang $lookup neu bang tin cham.
    private PostResponseCommand baseResponse(Post post, UUID viewerId) {
        return PostResponseCommand.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .status(post.getStatus())
                .sport(post.getSport())
                .sharedPostId(post.getSharedPostId())
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
                .savedByCurrentUser(viewerId != null
                        && reactionRepository.find(post.getPostId(), viewerId, ReactionType.SAVE).isPresent())
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
