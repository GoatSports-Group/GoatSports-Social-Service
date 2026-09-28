package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.domain.service.PostVisibilityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quan he cua nguoi xem va dung {@link PostResponseCommand} cho ca trang bai viet trong mot so truy van
 * co dinh (khong phu thuoc so bai): dem thich / binh luan / chia se va trang thai cua nguoi xem deu theo lo.
 */
@Component
@RequiredArgsConstructor
public class PostViewAssembler {
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final PostShareRepository shareRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final FollowRepository followRepository;

    /** Nguoi xem cung ban be va danh sach chan, tinh mot lan cho moi yeu cau. */
    public record Viewer(UUID id, Set<UUID> friendIds, Set<UUID> blockedIds) {
        public boolean canView(Post post) {
            return PostVisibilityPolicy.canView(post, id, friendIds, blockedIds);
        }
    }

    public Viewer viewer(UUID userId) {
        if (userId == null) return new Viewer(null, Set.of(), Set.of());
        Set<UUID> friends = new HashSet<>();
        for (Friendship friendship : friendshipRepository.findUserFriends(userId, FriendshipStatus.ACCEPTED)) {
            friends.add(userId.equals(friendship.getRequesterId()) ? friendship.getAddresseeId() : friendship.getRequesterId());
        }
        Set<UUID> blocked = new HashSet<>();
        for (UserBlock block : userBlockRepository.findRelatedToUser(userId)) {
            blocked.add(userId.equals(block.getBlockerId()) ? block.getBlockedUserId() : block.getBlockerId());
        }
        return new Viewer(userId, friends, blocked);
    }

    public Set<UUID> followingIds(UUID userId) {
        return followRepository.findByFollowerIdAndTargetType(userId, FollowTargetType.USER).stream()
                .map(Follow::getTargetId)
                .collect(Collectors.toSet());
    }

    public PostResponseCommand toResponse(Post post, Viewer viewer) {
        return toResponses(List.of(post), viewer).get(0);
    }

    public List<PostResponseCommand> toResponses(List<Post> posts, Viewer viewer) {
        if (posts.isEmpty()) return List.of();

        // Bai goc duoc kiem quyen lai cho tung nguoi xem, nen chia se khong lam lo bai da bi an.
        Set<UUID> sharedIds = posts.stream().map(Post::getSharedPostId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Post> originals = postRepository.findAllById(sharedIds).stream()
                .filter(original -> original.getStatus() == PostStatus.PUBLISHED && viewer.canView(original))
                .collect(Collectors.toMap(Post::getPostId, Function.identity()));

        Set<UUID> ids = new LinkedHashSet<>();
        posts.forEach(post -> ids.add(post.getPostId()));
        ids.addAll(originals.keySet());

        Counts counts = new Counts(
                reactionRepository.countByPostIds(ids, ReactionType.LIKE),
                commentRepository.countPublishedByPostIds(ids),
                merge(postRepository.countPublishedShares(ids), shareRepository.countByPostIds(ids)),
                myReactions(viewer, ids));

        List<PostResponseCommand> responses = new ArrayList<>(posts.size());
        for (Post post : posts) {
            PostResponseCommand response = build(post, counts);
            if (post.getSharedPostId() != null) {
                Post original = originals.get(post.getSharedPostId());
                response.setSharedPost(original == null ? null : build(original, counts));
                response.setSharedPostUnavailable(original == null);
            }
            responses.add(response);
        }
        return responses;
    }

    private record Counts(
            Map<UUID, Long> likes,
            Map<UUID, Long> comments,
            Map<UUID, Long> shares,
            Map<UUID, Set<ReactionType>> mine
    ) {
    }

    private Map<UUID, Set<ReactionType>> myReactions(Viewer viewer, Set<UUID> ids) {
        if (viewer.id() == null) return Map.of();
        Map<UUID, Set<ReactionType>> result = new HashMap<>();
        for (PostReaction reaction : reactionRepository.findByUserAndPostIds(viewer.id(), ids)) {
            result.computeIfAbsent(reaction.getPostId(), key -> new HashSet<>()).add(reaction.getType());
        }
        return result;
    }

    /** Luot chia se = bai chia se dang hien thi + ban ghi chia se kieu cu (truoc khi chia se thanh bai). */
    private static Map<UUID, Long> merge(Map<UUID, Long> left, Map<UUID, Long> right) {
        Map<UUID, Long> result = new HashMap<>(left);
        right.forEach((key, value) -> result.merge(key, value, Long::sum));
        return result;
    }

    private static PostResponseCommand build(Post post, Counts counts) {
        Set<ReactionType> mine = counts.mine().getOrDefault(post.getPostId(), Set.of());
        return PostResponseCommand.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .status(post.getStatus())
                .sport(post.getSport())
                .playerCall(post.getPlayerCall())
                .sharedPostId(post.getSharedPostId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .attachments(post.getAttachments().stream()
                        .sorted(Comparator.comparing(
                                attachment -> attachment.getDisplayOrder() == null ? 0 : attachment.getDisplayOrder()))
                        .map(attachment -> PostResponseCommand.AttachmentItem.builder()
                                .attachmentId(attachment.getAttachmentId())
                                .storageKey(attachment.getStorageKey())
                                .type(attachment.getType())
                                .displayOrder(attachment.getDisplayOrder())
                                .build())
                        .toList())
                .tags(post.getTags())
                .mentions(post.getMentions())
                .likeCount(counts.likes().getOrDefault(post.getPostId(), 0L))
                .commentCount(counts.comments().getOrDefault(post.getPostId(), 0L))
                .shareCount(counts.shares().getOrDefault(post.getPostId(), 0L))
                .likedByCurrentUser(mine.contains(ReactionType.LIKE))
                .savedByCurrentUser(mine.contains(ReactionType.SAVE))
                .build();
    }
}
