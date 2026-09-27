package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.port.external.StorageKeyPromoter;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.application.service.SocialPostService;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.domain.enums.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialPostServiceImplTest {
    @Mock private PostRepository postRepository;
    @Mock private PostCommentRepository commentRepository;
    @Mock private PostReactionRepository reactionRepository;
    @Mock private PostShareRepository shareRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserBlockRepository userBlockRepository;
    @Mock private FollowRepository followRepository;
    @Mock private StorageKeyPromoter storageKeyPromoter;
    @Mock private SocialEventPublisher eventPublisher;

    private SocialPostServiceImpl service;

    @BeforeEach
    void setUp() {
        // The assembler is real: tests exercise visibility and batched counting, only storage is mocked.
        PostViewAssembler assembler = new PostViewAssembler(postRepository, commentRepository, reactionRepository,
                shareRepository, friendshipRepository, userBlockRepository, followRepository);
        service = new SocialPostServiceImpl(postRepository, commentRepository, reactionRepository, assembler,
                storageKeyPromoter, eventPublisher);
        lenient().when(storageKeyPromoter.promote(org.mockito.ArgumentMatchers.anyList(), anyString()))
                .thenAnswer(call -> call.getArgument(0));
    }

    private static Post post(UUID authorId, PostVisibility visibility) {
        LocalDateTime now = LocalDateTime.now();
        return Post.builder()
                .postId(UUID.randomUUID())
                .authorId(authorId)
                .content("Kèo bóng 5 người tối thứ Sáu, còn thiếu 2 thủ môn")
                .visibility(visibility)
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .build();
    }

    private void savesReturnTheirArgument() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            if (saved.getPostId() == null) saved.setPostId(UUID.randomUUID());
            return saved;
        });
    }

    private Post captureSavedPost() {
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- create / update -------------------------------------------------------------------------

    @Test
    void createPostNormalizesContentAndPersistsAttachments() throws ServiceException {
        UUID actorId = UUID.randomUUID();
        savesReturnTheirArgument();

        service.createPost(SavePostCommand.builder()
                .actorId(actorId)
                .content("  Một buổi tập tuyệt vời  ")
                .visibility(PostVisibility.FRIENDS)
                .attachments(List.of(SavePostCommand.AttachmentItem.builder()
                        .storageKey("temp/social-posts/photo.jpg").type(AttachmentType.IMAGE).displayOrder(0).build()))
                .build());

        Post saved = captureSavedPost();
        assertThat(saved.getAuthorId()).isEqualTo(actorId);
        assertThat(saved.getContent()).isEqualTo("Một buổi tập tuyệt vời");
        assertThat(saved.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(saved.getAttachments()).singleElement()
                .satisfies(attachment -> assertThat(attachment.getType()).isEqualTo(AttachmentType.IMAGE));
    }

    @Test
    void createPostRejectsAnEmptyPost() {
        SavePostCommand command = SavePostCommand.builder()
                .actorId(UUID.randomUUID()).content("   ").visibility(PostVisibility.PUBLIC).attachments(List.of()).build();

        assertThatThrownBy(() -> service.createPost(command)).isInstanceOf(ServiceException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("bài viết lưu khóa vĩnh viễn, không lưu khóa temp/")
    void createPostStoresPromotedStorageKey() throws ServiceException {
        savesReturnTheirArgument();
        when(storageKeyPromoter.promote(org.mockito.ArgumentMatchers.anyList(), anyString()))
                .thenReturn(List.of("social-posts/photo.jpg"));

        service.createPost(SavePostCommand.builder()
                .actorId(UUID.randomUUID()).content("Có ảnh").visibility(PostVisibility.PUBLIC)
                .attachments(List.of(SavePostCommand.AttachmentItem.builder()
                        .storageKey("temp/social-posts/photo.jpg").type(AttachmentType.IMAGE).displayOrder(0).build()))
                .build());

        assertThat(captureSavedPost().getAttachments()).singleElement()
                .satisfies(attachment -> assertThat(attachment.getStorageKey()).isEqualTo("social-posts/photo.jpg"));
    }

    @Test
    void createPostExtractsHashtagsFromTheContent() throws ServiceException {
        savesReturnTheirArgument();

        PostResponseCommand response = service.createPost(SavePostCommand.builder()
                .actorId(UUID.randomUUID())
                .content("Tối nay đá #BóngĐá sân 7, ai vào #keo_toi #bóngđá nữa không")
                .visibility(PostVisibility.PUBLIC)
                .build());

        assertThat(response.getTags()).containsExactly("bóngđá", "keo_toi");
    }

    @Test
    @DisplayName("nhắc tên: bỏ chính mình, người bị chặn; chỉ báo người xem được bài")
    void mentionsSkipSelfBlockedAndPeopleWhoCannotSeeThePost() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        savesReturnTheirArgument();
        when(userBlockRepository.findRelatedToUser(author))
                .thenReturn(List.of(UserBlock.builder().blockerId(author).blockedUserId(blocked).build()));
        when(friendshipRepository.findUserFriends(any(), any())).thenAnswer(call -> friend.equals(call.getArgument(0))
                ? List.of(iuh.fit.socialservice.domain.entity.Friendship.builder().requesterId(friend).addresseeId(author).build())
                : List.of());

        PostResponseCommand response = service.createPost(SavePostCommand.builder()
                .actorId(author)
                .content("Cảm ơn team hôm nay!")
                .visibility(PostVisibility.FRIENDS)
                .mentions(List.of(friend, stranger, author, blocked, friend))
                .build());

        assertThat(response.getMentions()).containsExactly(friend, stranger);
        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_MENTIONED), eq(friend), eq(author), any(), anyMap(),
                anyString());
        // Bai chi ban be xem: nguoi la duoc nhac nhung khong xem duoc thi khong bao.
        verify(eventPublisher, never()).publish(anyString(), eq(stranger), any(), any(), anyMap(), any());
    }

    @Test
    void editingAPostDoesNotRenotifyPeopleAlreadyMentioned() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Post existing = post(author, PostVisibility.PUBLIC);
        existing.setMentionedUserIds(List.of(first));
        when(postRepository.findById(existing.getPostId())).thenReturn(Optional.of(existing));
        savesReturnTheirArgument();

        service.updatePost(existing.getPostId(), SavePostCommand.builder()
                .actorId(author).content("Cập nhật đội hình").visibility(PostVisibility.PUBLIC)
                .mentions(List.of(first, second)).build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_MENTIONED), eq(second), any(), any(), anyMap(), anyString());
        verify(eventPublisher, never()).publish(anyString(), eq(first), any(), any(), anyMap(), any());
    }

    // ---- visibility & reactions ------------------------------------------------------------------

    @Test
    @DisplayName("người bị chặn không thích được bài công khai của người đã chặn mình")
    void blockedViewerCannotLikeAPublicPost() {
        UUID author = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(userBlockRepository.findRelatedToUser(viewer))
                .thenReturn(List.of(UserBlock.builder().blockerId(author).blockedUserId(viewer).build()));

        assertThatThrownBy(() -> service.likePost(target.getPostId(), viewer)).isInstanceOf(ServiceException.class);
        verify(reactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(anyString(), any(), any(), any(), anyMap(), any());
    }

    @Test
    void likePostIsIdempotent() throws ServiceException {
        UUID actorId = UUID.randomUUID();
        Post target = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(reactionRepository.find(target.getPostId(), actorId, ReactionType.LIKE))
                .thenReturn(Optional.of(PostReaction.builder().build()));

        service.likePost(target.getPostId(), actorId);

        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("thích lại dùng cùng khóa chống trùng, nên tác giả không bị báo lần hai")
    void likeNotificationCarriesAStableDedupeKeyAndSkipsTheActorThemself() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID fan = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));

        service.likePost(target.getPostId(), fan);
        service.likePost(target.getPostId(), author);

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_LIKED), eq(author), eq(fan), eq(target.getPostId()),
                anyMap(), eq("like:" + target.getPostId() + ":" + fan));
        verify(eventPublisher, never()).publish(anyString(), eq(author), eq(author), any(), anyMap(), any());
    }

    @Test
    void replyNotifiesBothThePostAuthorAndTheParentCommentAuthor() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID firstCommenter = UUID.randomUUID();
        UUID replier = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        PostComment parent = PostComment.builder()
                .commentId(UUID.randomUUID()).postId(target.getPostId()).authorId(firstCommenter)
                .status(PostStatus.PUBLISHED).build();
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(commentRepository.findById(parent.getCommentId())).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(PostComment.class))).thenAnswer(call -> call.getArgument(0));

        service.createComment(target.getPostId(), SaveCommentCommand.builder()
                .actorId(replier).parentCommentId(parent.getCommentId()).content("Mình vào sân được lúc 19h").build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_COMMENTED), eq(author), eq(replier), any(), anyMap(), isNull());
        verify(eventPublisher).publish(eq(SocialEventPublisher.COMMENT_REPLIED), eq(firstCommenter), eq(replier), any(), anyMap(), isNull());
    }

    // ---- share -----------------------------------------------------------------------------------

    @Test
    @DisplayName("chia sẻ lại một bài chia sẻ trỏ về bài gốc, giữ môn thể thao của bài gốc")
    void resharingPointsAtTheOriginalPost() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID sharer = UUID.randomUUID();
        Post original = post(author, PostVisibility.PUBLIC);
        original.setSport(PostSport.BADMINTON);
        Post firstShare = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        firstShare.setSharedPostId(original.getPostId());
        when(postRepository.findById(firstShare.getPostId())).thenReturn(Optional.of(firstShare));
        when(postRepository.findById(original.getPostId())).thenReturn(Optional.of(original));
        when(postRepository.findAllById(any())).thenReturn(List.of(original));
        savesReturnTheirArgument();

        PostResponseCommand share = service.sharePost(firstShare.getPostId(), SharePostCommand.builder()
                .actorId(sharer).caption("  Ai rảnh vào đội mình #caulong  ").build());

        assertThat(share.getSharedPostId()).isEqualTo(original.getPostId());
        assertThat(share.getSport()).isEqualTo(PostSport.BADMINTON);
        assertThat(share.getContent()).isEqualTo("Ai rảnh vào đội mình #caulong");
        assertThat(share.getTags()).containsExactly("caulong");
        assertThat(share.getSharedPost().getPostId()).isEqualTo(original.getPostId());
        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_SHARED), eq(author), eq(sharer),
                eq(original.getPostId()), anyMap(), isNull());
    }

    @Test
    void onlyPublicPostsCanBeShared() {
        UUID author = UUID.randomUUID();
        Post friendsOnly = post(author, PostVisibility.FRIENDS);
        when(postRepository.findById(friendsOnly.getPostId())).thenReturn(Optional.of(friendsOnly));

        assertThatThrownBy(() -> service.sharePost(friendsOnly.getPostId(),
                SharePostCommand.builder().actorId(author).build())).isInstanceOf(ServiceException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("bài gốc bị ẩn thì bài chia sẻ không làm lộ nội dung")
    void hiddenOriginalIsNotEmbeddedInAShare() throws ServiceException {
        Post original = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        original.setStatus(PostStatus.HIDDEN);
        Post share = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        share.setSharedPostId(original.getPostId());
        when(postRepository.findById(share.getPostId())).thenReturn(Optional.of(share));
        when(postRepository.findAllById(any())).thenReturn(List.of(original));

        PostResponseCommand response = service.getPost(share.getPostId(), UUID.randomUUID());

        assertThat(response.getSharedPost()).isNull();
        assertThat(response.isSharedPostUnavailable()).isTrue();
    }

    // ---- feed ------------------------------------------------------------------------------------

    @Test
    @DisplayName("một trang bảng tin đếm theo lô: số truy vấn không tăng theo số bài")
    void feedCountsInOneBatchPerPage() {
        UUID viewer = UUID.randomUUID();
        Post a = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        Post b = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        Post c = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        PageRequest page = PageRequest.of(0, 10);
        when(postRepository.findFeed(any(), eq(page))).thenReturn(new PageImpl<>(List.of(a, b, c), page, 3));
        when(reactionRepository.countByPostIds(any(), eq(ReactionType.LIKE))).thenReturn(Map.of(b.getPostId(), 4L));
        when(postRepository.countPublishedShares(any())).thenReturn(Map.of(b.getPostId(), 1L));
        when(shareRepository.countByPostIds(any())).thenReturn(Map.of(b.getPostId(), 2L));
        when(reactionRepository.findByUserAndPostIds(eq(viewer), any())).thenReturn(List.of(
                PostReaction.builder().postId(b.getPostId()).userId(viewer).type(ReactionType.SAVE).build()));

        List<PostResponseCommand> feed = service.getFeed(viewer,
                new SocialPostService.FeedFilter(false, null, null, null), page).getContent();

        assertThat(feed).hasSize(3);
        assertThat(feed.get(1).getLikeCount()).isEqualTo(4);
        assertThat(feed.get(1).getShareCount()).as("share posts + legacy share rows").isEqualTo(3);
        assertThat(feed.get(1).isSavedByCurrentUser()).isTrue();
        assertThat(feed.get(1).isLikedByCurrentUser()).isFalse();
        verify(reactionRepository, times(1)).countByPostIds(any(), eq(ReactionType.LIKE));
        verify(commentRepository, times(1)).countPublishedByPostIds(any());
        verify(reactionRepository, times(1)).findByUserAndPostIds(eq(viewer), any());
    }

    @Test
    void tagFilterIsNormalizedBeforeQuerying() {
        PageRequest page = PageRequest.of(0, 10);
        when(postRepository.findFeed(any(), eq(page))).thenReturn(new PageImpl<>(List.of(), page, 0));

        service.getFeed(null, new SocialPostService.FeedFilter(false, null, null, "#BóngĐá"), page);

        ArgumentCaptor<PostRepository.FeedQuery> query = ArgumentCaptor.forClass(PostRepository.FeedQuery.class);
        verify(postRepository).findFeed(query.capture(), eq(page));
        assertThat(query.getValue().tag()).isEqualTo("bóngđá");
    }

    @Test
    void savedPostsQueryCarriesTheViewersFriendsAndBlocks() throws ServiceException {
        UUID viewer = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        PageRequest page = PageRequest.of(0, 10);
        when(userBlockRepository.findRelatedToUser(viewer))
                .thenReturn(List.of(UserBlock.builder().blockerId(viewer).blockedUserId(blocked).build()));
        when(postRepository.findSaved(any(), eq(page))).thenReturn(new PageImpl<>(List.of(), page, 0));

        service.getSavedPosts(viewer, page);

        ArgumentCaptor<PostRepository.FeedQuery> query = ArgumentCaptor.forClass(PostRepository.FeedQuery.class);
        verify(postRepository).findSaved(query.capture(), eq(page));
        assertThat(query.getValue().viewerId()).isEqualTo(viewer);
        assertThat(query.getValue().blockedUserIds()).containsExactly(blocked);
    }

    @Test
    void snippetFlattensWhitespaceAndCapsTheLength() {
        assertThat(SocialPostServiceImpl.snippet("  a\n\n b  ")).isEqualTo("a b");
        assertThat(SocialPostServiceImpl.snippet("x".repeat(200))).hasSize(80).endsWith("…");
        assertThat(SocialPostServiceImpl.snippet(null)).isEmpty();
    }
}
