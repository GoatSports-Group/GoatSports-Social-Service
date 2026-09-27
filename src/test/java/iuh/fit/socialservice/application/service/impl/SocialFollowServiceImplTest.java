package iuh.fit.socialservice.application.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
    private SocialEventPublisher eventPublisher;

    private SocialFollowServiceImpl service;

    private final UUID follower = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SocialFollowServiceImpl(
                followRepository, userBlockRepository, friendshipRepository, postRepository, eventPublisher);
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
    @SuppressWarnings("unchecked")
    void suggestionsExcludeSelfFollowedFriendsAndBlockedPeople() throws Exception {
        UUID followed = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID blocker = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndTargetType(follower, FollowTargetType.USER))
                .thenReturn(List.of(Follow.builder().targetId(followed).build()));
        when(friendshipRepository.findUserFriends(follower, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(Friendship.builder().requesterId(friend).addresseeId(follower).build()));
        when(userBlockRepository.findRelatedToUser(follower))
                .thenReturn(List.of(UserBlock.builder().blockerId(blocker).blockedUserId(follower).build()));
        when(postRepository.findActiveAuthors(any(), any(), anyInt())).thenReturn(List.of());

        service.getFollowSuggestions(follower, 50);

        ArgumentCaptor<Collection<UUID>> excluded = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(postRepository).findActiveAuthors(any(), excluded.capture(), limit.capture());
        assertThat(excluded.getValue()).contains(follower, followed, friend, blocker);
        assertThat(limit.getValue()).isEqualTo(10);
    }
}
