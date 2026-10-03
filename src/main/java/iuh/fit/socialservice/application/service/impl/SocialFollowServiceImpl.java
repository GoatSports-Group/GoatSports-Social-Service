package iuh.fit.socialservice.application.service.impl;

import java.util.HashMap;
import iuh.fit.socialservice.domain.service.FollowSuggestionRanker;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.domain.enums.PostSport;
import java.util.ArrayList;
import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.application.service.SocialFollowService;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialFollowServiceImpl implements SocialFollowService {
    private final FollowRepository followRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;
    private final PostRepository postRepository;
    private final ConversationRepository conversationRepository;

    /** Nhom chat noi len moi lien he that; nhom chat tu tao (GROUP) thi khong. */
    private static final Set<ConversationType> RELATED_CHATS =
            Set.of(ConversationType.MATCH, ConversationType.CLUB, ConversationType.DIRECT, ConversationType.TOURNAMENT);
    /** So nguoi nang no lay ra de tron vao xep hang (chi de pha hoa). */
    private static final int CANDIDATE_POOL = 30;
    private final SocialEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public VenueFollowStatusResponseCommand getVenueFollowStatus(UUID venueId, UUID followerId) {
        boolean followed = followerId != null && findFollow(followerId, FollowTargetType.VENUE, venueId) != null;
        return venueResponse(venueId, followed);
    }

    @Override
    @Transactional
    public VenueFollowStatusResponseCommand followVenue(UUID venueId, UUID followerId) {
        if (findFollow(followerId, FollowTargetType.VENUE, venueId) == null) {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .targetType(FollowTargetType.VENUE)
                    .targetId(venueId)
                    .build());
        }
        return venueResponse(venueId, true);
    }

    @Override
    @Transactional
    public VenueFollowStatusResponseCommand unfollowVenue(UUID venueId, UUID followerId) {
        Follow existing = findFollow(followerId, FollowTargetType.VENUE, venueId);
        if (existing != null) followRepository.deleteById(existing.getFollowId());
        return venueResponse(venueId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public UserFollowStatusResponseCommand getUserFollowStatus(UUID targetUserId, UUID followerId) {
        boolean followed = followerId != null
                && findFollow(followerId, FollowTargetType.USER, targetUserId) != null;
        return userResponse(targetUserId, followed);
    }

    @Override
    @Transactional
    public UserFollowStatusResponseCommand followUser(UUID targetUserId, UUID followerId)
            throws ServiceException {
        requireUsers(targetUserId, followerId);
        if (followerId.equals(targetUserId)) {
            throw new ServiceException("Bạn không thể theo dõi chính mình");
        }
        if (blockedEitherWay(followerId, targetUserId)) {
            throw new ServiceException("Không thể theo dõi người dùng này");
        }
        if (findFollow(followerId, FollowTargetType.USER, targetUserId) == null) {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .targetType(FollowTargetType.USER)
                    .targetId(targetUserId)
                    .build());
            eventPublisher.publish(SocialEventPublisher.USER_FOLLOWED, targetUserId, followerId, followerId,
                    Map.of("followerId", followerId.toString()),
                    // Bo theo doi roi theo doi lai khong bao lan nua.
                    "follow:" + followerId + ":" + targetUserId);
        }
        return userResponse(targetUserId, true);
    }

    @Override
    @Transactional
    public UserFollowStatusResponseCommand unfollowUser(UUID targetUserId, UUID followerId)
            throws ServiceException {
        requireUsers(targetUserId, followerId);
        Follow existing = findFollow(followerId, FollowTargetType.USER, targetUserId);
        if (existing != null) followRepository.deleteById(existing.getFollowId());
        return userResponse(targetUserId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedVenue> getSavedVenues(UUID followerId) {
        if (followerId == null) return List.of();
        // ponytail: sap xep trong bo nho tren toan bo danh sach luu; them phan trang o DB neu mot nguoi luu hang nghin san.
        return followRepository.findByFollowerIdAndTargetType(followerId, FollowTargetType.VENUE).stream()
                .sorted(Comparator.comparing(Follow::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(follow -> new SavedVenue(follow.getTargetId(), follow.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getFollowingUserIds(UUID followerId) {
        if (followerId == null) return List.of();
        return followRepository.findByFollowerIdAndTargetType(followerId, FollowTargetType.USER).stream()
                .map(Follow::getTargetId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowSuggestion> getFollowSuggestions(UUID viewerId, Set<PostSport> sports, int limit)
            throws ServiceException {
        if (viewerId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        int size = Math.max(1, Math.min(limit, 10));
        Set<UUID> friends = new HashSet<>();
        friendshipRepository.findUserFriends(viewerId, FriendshipStatus.ACCEPTED).forEach(friendship ->
                friends.add(viewerId.equals(friendship.getRequesterId()) ? friendship.getAddresseeId() : friendship.getRequesterId()));
        Set<UUID> excluded = new HashSet<>(getFollowingUserIds(viewerId));
        excluded.add(viewerId);
        excluded.addAll(friends);
        userBlockRepository.findRelatedToUser(viewerId).forEach(block -> {
            excluded.add(block.getBlockerId());
            excluded.add(block.getBlockedUserId());
        });

        FollowSuggestionRanker ranker = new FollowSuggestionRanker(excluded);
        for (ConversationRepository.CoMember member : conversationRepository.findCoMembers(viewerId, RELATED_CHATS)) {
            switch (member.type()) {
                case MATCH -> ranker.playedWith(member.userId());
                case CLUB -> ranker.sameClub(member.userId(), member.conversationName());
                case DIRECT -> ranker.chattedWith(member.userId());
                case TOURNAMENT -> ranker.sameTournament(member.userId(), member.conversationName());
                default -> { }
            }
        }
        // Ban chung: dem moi nguoi la ban cua bao nhieu ban cua minh.
        Map<UUID, Integer> mutual = new HashMap<>();
        for (Friendship friendship : friendshipRepository.findAcceptedOf(friends)) {
            UUID other = friends.contains(friendship.getRequesterId()) ? friendship.getAddresseeId() : friendship.getRequesterId();
            mutual.merge(other, 1, Integer::sum);
        }
        mutual.forEach(ranker::mutualFriends);

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        if (sports != null && !sports.isEmpty()) {
            postRepository.findActiveAuthors(since, excluded, sports, CANDIDATE_POOL)
                    .forEach(item -> ranker.postsInMySports(item.authorId(), item.postCount()));
        }
        postRepository.findActiveAuthors(since, excluded, Set.of(), CANDIDATE_POOL)
                .forEach(item -> ranker.activePoster(item.authorId(), item.postCount()));
        return ranker.top(size).stream()
                .map(item -> new FollowSuggestion(item.userId(), item.postCount(), item.reason()))
                .toList();
    }

    private void requireUsers(UUID targetUserId, UUID followerId) throws ServiceException {
        if (followerId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        if (targetUserId == null) throw new ServiceException("Người dùng cần theo dõi không hợp lệ");
    }

    private boolean blockedEitherWay(UUID followerId, UUID targetUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedUserId(followerId, targetUserId)
                || userBlockRepository.existsByBlockerIdAndBlockedUserId(targetUserId, followerId);
    }

    private Follow findFollow(UUID followerId, FollowTargetType targetType, UUID targetId) {
        if (followerId == null || targetId == null) return null;
        return followRepository.findByFollowerIdAndTargetTypeAndTargetId(followerId, targetType, targetId)
                .orElse(null);
    }

    private VenueFollowStatusResponseCommand venueResponse(UUID venueId, boolean followed) {
        return VenueFollowStatusResponseCommand.builder()
                .venueId(venueId)
                .followed(followed)
                .build();
    }

    private UserFollowStatusResponseCommand userResponse(UUID targetUserId, boolean followed) {
        return UserFollowStatusResponseCommand.builder()
                .userId(targetUserId)
                .followed(followed)
                .followerCount(followRepository.countByTargetTypeAndTargetId(FollowTargetType.USER, targetUserId))
                .followingCount(followRepository
                        .findByFollowerIdAndTargetType(targetUserId, FollowTargetType.USER).size())
                .build();
    }
}
