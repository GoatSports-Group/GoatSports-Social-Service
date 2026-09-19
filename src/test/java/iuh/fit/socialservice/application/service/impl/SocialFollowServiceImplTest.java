package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.domain.entity.Follow;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private SocialFollowServiceImpl service;

    private final UUID follower = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SocialFollowServiceImpl(followRepository, userBlockRepository);
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
}
