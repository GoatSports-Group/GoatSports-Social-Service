package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.application.service.SocialFollowService;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.domain.enums.PostSport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialFollowServiceImplTest {
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private SocialEventPublisher eventPublisher;

    private SocialFollowServiceImpl service;

    private final UUID follower = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SocialFollowServiceImpl(
                followRepository, userBlockRepository, friendshipRepository, postRepository, conversationRepository,
                eventPublisher);
        lenient().when(followRepository.countByTargetTypeAndTargetId(any(), any())).thenReturn(0L);
        lenient().when(followRepository.findByFollowerIdAndTargetType(any(), any())).thenReturn(List.of());
    }

    @Test
    void followUserPersistsFollowWhenNotBlocked() throws Exception {
        when(followRepository.findByFollowerIdAndTargetTypeAndTargetId(follower, FollowTargetType.USER, target))
                .thenReturn(Optional.empty());

        UserFollowStatusResponseCommand response = service.followUser(target, follower);

        assertThat(response.isFollowed()).isTrue();
        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void followUserIsIdempotent() throws Exception {
        when(followRepository.findByFollowerIdAndTargetTypeAndTargetId(follower, FollowTargetType.USER, target))
                .thenReturn(Optional.of(Follow.builder().followId(UUID.randomUUID()).build()));

        UserFollowStatusResponseCommand response = service.followUser(target, follower);

        assertThat(response.isFollowed()).isTrue();
        verify(followRepository, never()).save(any());
    }

    @Test
    void cannotFollowSelf() {
        assertThatThrownBy(() -> service.followUser(follower, follower))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("chính mình");
        verify(followRepository, never()).save(any());
    }

    @Test
    void blockerCannotFollowBlockedUser() {
        when(userBlockRepository.existsByBlockerIdAndBlockedUserId(follower, target)).thenReturn(true);

        assertThatThrownBy(() -> service.followUser(target, follower))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Không thể theo dõi");
        verify(followRepository, never()).save(any());
    }

    @Test
    void blockedUserCannotFollowTheBlocker() {
        when(userBlockRepository.existsByBlockerIdAndBlockedUserId(follower, target)).thenReturn(false);
        when(userBlockRepository.existsByBlockerIdAndBlockedUserId(target, follower)).thenReturn(true);

        assertThatThrownBy(() -> service.followUser(target, follower))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Không thể theo dõi");
        verify(followRepository, never()).save(any());
    }

    @Test
    void unfollowRemovesExistingFollow() throws Exception {
        UUID followId = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndTargetTypeAndTargetId(follower, FollowTargetType.USER, target))
                .thenReturn(Optional.of(Follow.builder().followId(followId).build()));

        UserFollowStatusResponseCommand response = service.unfollowUser(target, follower);

        assertThat(response.isFollowed()).isFalse();
        verify(followRepository).deleteById(followId);
    }

    @Test
    void followNotifiesTheTargetOnlyTheFirstTime() throws Exception {
        when(followRepository.findByFollowerIdAndTargetTypeAndTargetId(follower, FollowTargetType.USER, target))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(Follow.builder().followId(UUID.randomUUID()).build()));

        service.followUser(target, follower);
        service.followUser(target, follower);

        verify(eventPublisher).publish(eq(SocialEventPublisher.USER_FOLLOWED), eq(target), eq(follower), any(), anyMap(),
                eq("follow:" + follower + ":" + target));
    }

    @Test
    void suggestionsNeverIncludeSelfFriendsPendingRequestsOrBlockedPeopleWhateverTheSignal() throws Exception {
        UUID followed = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID requested = UUID.randomUUID();
        UUID blocker = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        lenient().when(followRepository.findByFollowerIdAndTargetType(follower, FollowTargetType.USER))
                .thenReturn(List.of(Follow.builder().targetId(followed).build()));
        when(friendshipRepository.findUserFriends(follower, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(Friendship.builder().requesterId(friend).addresseeId(follower).build()));
        when(friendshipRepository.findUserFriends(follower, FriendshipStatus.PENDING))
                .thenReturn(List.of(Friendship.builder().requesterId(follower).addresseeId(requested).build()));
        when(userBlockRepository.findRelatedToUser(follower))
                .thenReturn(List.of(UserBlock.builder().blockerId(blocker).blockedUserId(follower).build()));
        // Ai cung chung nhom chat CLB voi minh, ke ca nhung nguoi phai loai.
        when(conversationRepository.findCoMembers(eq(follower), any())).thenReturn(List.of(
                new ConversationRepository.CoMember(followed, ConversationType.CLUB, "Weekend Club"),
                new ConversationRepository.CoMember(friend, ConversationType.CLUB, "Weekend Club"),
                new ConversationRepository.CoMember(requested, ConversationType.CLUB, "Weekend Club"),
                new ConversationRepository.CoMember(blocker, ConversationType.CLUB, "Weekend Club"),
                new ConversationRepository.CoMember(stranger, ConversationType.CLUB, "Weekend Club")));
        when(postRepository.findActiveAuthors(any(), any(), any(), anyInt()))
                .thenReturn(List.of(new PostRepository.AuthorActivity(follower, 3)));

        // Nguoi da theo doi (cu) van duoc goi y ket ban.
        assertThat(service.getFollowSuggestions(follower, null, 50, 0))
                .extracting(SocialFollowService.FollowSuggestion::authorId).containsExactlyInAnyOrder(followed, stranger);
    }

    @Test
    void realConnectionsOutrankBusyPostersAndExplainThemselves() throws Exception {
        UUID friend = UUID.randomUUID();
        UUID opponent = UUID.randomUUID();
        UUID clubmate = UUID.randomUUID();
        UUID busyPoster = UUID.randomUUID();
        when(friendshipRepository.findUserFriends(follower, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(Friendship.builder().requesterId(follower).addresseeId(friend).build()));
        when(conversationRepository.findCoMembers(eq(follower), any())).thenReturn(List.of(
                new ConversationRepository.CoMember(opponent, ConversationType.MATCH, "Kèo BADMINTON"),
                new ConversationRepository.CoMember(clubmate, ConversationType.CLUB, "Weekend Club")));
        // Doi thu cung la ban cua ban minh: them "1 ban chung".
        when(friendshipRepository.findAcceptedOf(Set.of(friend)))
                .thenReturn(List.of(Friendship.builder().requesterId(friend).addresseeId(opponent).build()));
        when(postRepository.findActiveAuthors(any(), any(), eq(Set.of(PostSport.BADMINTON)), anyInt()))
                .thenReturn(List.of(new PostRepository.AuthorActivity(clubmate, 2)));
        when(postRepository.findActiveAuthors(any(), any(), eq(Set.of()), anyInt()))
                .thenReturn(List.of(new PostRepository.AuthorActivity(busyPoster, 9),
                        new PostRepository.AuthorActivity(clubmate, 2)));

        List<SocialFollowService.FollowSuggestion> suggestions =
                service.getFollowSuggestions(follower, Set.of(PostSport.BADMINTON), 5, 0);

        assertThat(suggestions).extracting(SocialFollowService.FollowSuggestion::authorId)
                .containsExactly(opponent, clubmate, busyPoster);
        assertThat(suggestions.get(0).reason()).isEqualTo("Đã ghép trận với bạn · 1 bạn chung");
        assertThat(suggestions.get(1).reason()).isEqualTo("Cùng CLB Weekend Club · Hay đăng bài về môn bạn chơi");
        assertThat(suggestions.get(2).reason()).isEqualTo("9 bài trong 30 ngày");
        assertThat(suggestions.get(2).postCount()).isEqualTo(9);
    }

    @Test
    void seeAllSuggestionsPagesWithAnOffset() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(postRepository.findActiveAuthors(any(), any(), eq(Set.of()), anyInt())).thenReturn(List.of(
                new PostRepository.AuthorActivity(first, 9), new PostRepository.AuthorActivity(second, 3)));

        assertThat(service.getFollowSuggestions(follower, null, 1, 1))
                .extracting(SocialFollowService.FollowSuggestion::authorId).containsExactly(second);
    }

    @Test
    void savedVenuesAreNewestFirstAndOnlyVenues() {
        UUID older = UUID.randomUUID();
        UUID newer = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndTargetType(follower, FollowTargetType.VENUE)).thenReturn(List.of(
                Follow.builder().followerId(follower).targetType(FollowTargetType.VENUE).targetId(older)
                        .createdAt(LocalDateTime.of(2026, 9, 1, 8, 0)).build(),
                Follow.builder().followerId(follower).targetType(FollowTargetType.VENUE).targetId(newer)
                        .createdAt(LocalDateTime.of(2026, 10, 1, 8, 0)).build()));

        assertThat(service.getSavedVenues(follower))
                .extracting(saved -> saved.venueId())
                .containsExactly(newer, older);
        assertThat(service.getSavedVenues(null)).isEmpty();
    }
}
