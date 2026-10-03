package iuh.fit.socialservice.application.service.impl;

import java.util.Set;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.domain.enums.FollowTargetType;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.entity.Follow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import iuh.fit.socialservice.application.dto.request.PublishHighlightCommand;
import iuh.fit.socialservice.domain.enums.HighlightKind;
import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.external.ClubDirectory;
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
import iuh.fit.socialservice.domain.entity.Mention;
import iuh.fit.socialservice.domain.entity.PlayerCall;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    @Mock private ClubDirectory clubDirectory;

    private SocialPostServiceImpl service;

    @BeforeEach
    void setUp() {
        // The assembler is real: tests exercise visibility and batched counting, only storage is mocked.
        PostViewAssembler assembler = new PostViewAssembler(postRepository, commentRepository, reactionRepository,
                shareRepository, friendshipRepository, userBlockRepository, followRepository);
        service = new SocialPostServiceImpl(postRepository, commentRepository, reactionRepository, assembler,
                storageKeyPromoter, eventPublisher, clubDirectory);
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

    @Test
    void followingFeedShowsOnlyPeopleIFollowNeverMyOwnPosts() {
        UUID me = UUID.randomUUID();
        UUID followed = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndTargetType(me, FollowTargetType.USER))
                .thenReturn(List.of(Follow.builder().targetId(followed).build()));
        when(friendshipRepository.findUserFriends(me, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(Friendship.builder().requesterId(me).addresseeId(friend).build()));
        when(postRepository.findFeed(any(), any())).thenReturn(Page.empty());
        var page = PageRequest.of(0, 10);

        service.getFeed(me, new SocialPostService.FeedFilter(true, null, null, null), page);
        service.getFeed(me, new SocialPostService.FeedFilter(true, null, null, null, false, Set.of(), true), page);

        ArgumentCaptor<PostRepository.FeedQuery> captor = ArgumentCaptor.forClass(PostRepository.FeedQuery.class);
        verify(postRepository, org.mockito.Mockito.times(2)).findFeed(captor.capture(), any());
        assertThat(captor.getAllValues().get(0).followingIds()).containsExactly(followed);
        // Khoi "Ban be dang hoat dong" o Trang chu: kem ban be, van khong co chinh minh.
        assertThat(captor.getAllValues().get(1).followingIds()).containsExactlyInAnyOrder(followed, friend);
    }

    // ---- create / update -------------------------------------------------------------------------

    @Test
    void highlightIsPublicTagsTheRightPeopleAndNotifiesEachOnce() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID opponent = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        savesReturnTheirArgument();
        when(userBlockRepository.findRelatedToUser(author))
                .thenReturn(List.of(UserBlock.builder().blockerId(author).blockedUserId(blocked).build()));
        PostHighlight highlight = new PostHighlight(HighlightKind.MATCH, refId, "  Trận cầu lông đã nhất tuần  ",
                "Cầu lông · Đánh đơn", "Thứ Sáu, 02/10/2026", "Thắng 21–15",
                List.of(new PostHighlight.Side("Minh Đạt", 21, true), new PostHighlight.Side("Thắng Đạt", 15, false)),
                List.of(), null, null, List.of(opponent, author, blocked, opponent));

        service.publishHighlight(new PublishHighlightCommand(author, PostSport.BADMINTON, " GG! ", highlight));

        Post saved = captureSavedPost();
        assertThat(saved.getVisibility()).isEqualTo(PostVisibility.PUBLIC);
        assertThat(saved.getContent()).isEqualTo("GG!");
        assertThat(saved.getHighlight().title()).isEqualTo("Trận cầu lông đã nhất tuần");
        assertThat(saved.getHighlight().tagged()).containsExactly(opponent);
        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_TAGGED), eq(opponent), eq(author),
                eq(saved.getPostId()), any(), eq("tagged:" + saved.getPostId() + ":" + opponent));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void aClubShowsOffATournamentOnlyOnce() {
        UUID clubId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        when(postRepository.existsHighlight(eq(HighlightKind.TOURNAMENT), eq(tournamentId), any(), eq(clubId)))
                .thenReturn(true);
        PostHighlight highlight = new PostHighlight(HighlightKind.TOURNAMENT, tournamentId, "Vô địch!", "Giải A", null,
                "Vô địch", List.of(), List.of(), clubId, "Weekend Club", List.of());

        assertThatThrownBy(() -> service.publishHighlight(
                new PublishHighlightCommand(UUID.randomUUID(), PostSport.FOOTBALL, null, highlight)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đã khoe giải này");
        verify(postRepository, never()).save(any());
    }


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

    private static PlayerCall call(UUID clubId) {
        return new PlayerCall(PostSport.FOOTBALL, "FOOTBALL_7", LocalDate.now().plusDays(1), LocalTime.of(19, 0),
                LocalTime.of(21, 0), "Sân Chảo Lửa, Tân Bình", 2, "Trung bình", clubId, "Tên do client tự đặt", true);
    }

    @Test
    @DisplayName("kèo tìm người chơi: luôn công khai, gắn môn của kèo, không cần lời nhắn; không tin trạng thái client gửi")
    void playerCallIsAlwaysPublic() throws ServiceException {
        savesReturnTheirArgument();

        PostResponseCommand response = service.createPost(SavePostCommand.builder()
                .actorId(UUID.randomUUID()).visibility(PostVisibility.FRIENDS).playerCall(call(null)).build());

        Post saved = captureSavedPost();
        assertThat(saved.getVisibility()).isEqualTo(PostVisibility.PUBLIC);
        assertThat(saved.getSport()).isEqualTo(PostSport.FOOTBALL);
        assertThat(saved.getPlayerCall().filled()).isFalse();
        assertThat(saved.getPlayerCall().clubName()).isNull();
        assertThat(response.getPlayerCall().slots()).isEqualTo(2);
    }

    @Test
    void playerCallInThePastIsRejected() {
        PlayerCall past = new PlayerCall(PostSport.BADMINTON, null, LocalDate.now().minusDays(1), LocalTime.of(19, 0),
                LocalTime.of(21, 0), "Quận 7", 1, null, null, null, false);

        assertThatThrownBy(() -> service.createPost(SavePostCommand.builder()
                .actorId(UUID.randomUUID()).visibility(PostVisibility.PUBLIC).playerCall(past).build()))
                .isInstanceOf(ServiceException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("kèo thay CLB: chỉ chủ/quản lý đăng được, tên CLB lấy từ club-service")
    void clubPlayerCallNeedsAManager() throws ServiceException {
        UUID manager = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(clubDirectory.find(clubId)).thenReturn(Optional.of(
                new ClubDirectory.Club(clubId, "FC Bến Nghé", PostSport.FOOTBALL, List.of(manager))));

        assertThatThrownBy(() -> service.createPost(SavePostCommand.builder()
                .actorId(UUID.randomUUID()).visibility(PostVisibility.PUBLIC).playerCall(call(clubId)).build()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("Chỉ chủ CLB hoặc quản lý");
        verify(postRepository, never()).save(any());

        savesReturnTheirArgument();
        service.createPost(SavePostCommand.builder()
                .actorId(manager).visibility(PostVisibility.PUBLIC).playerCall(call(clubId)).build());
        assertThat(captureSavedPost().getPlayerCall().clubName()).isEqualTo("FC Bến Nghé");
    }

    @Test
    void onlyTheAuthorMarksAPlayerCallFilled() throws ServiceException {
        UUID author = UUID.randomUUID();
        Post post = post(author, PostVisibility.PUBLIC);
        post.setPlayerCall(call(null).withFilled(false));
        when(postRepository.findById(post.getPostId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.setPlayerCallFilled(post.getPostId(), UUID.randomUUID(), true))
                .isInstanceOf(ServiceException.class);

        savesReturnTheirArgument();
        assertThat(service.setPlayerCallFilled(post.getPostId(), author, true).getPlayerCall().filled()).isTrue();
    }

    @Test
    void openPlayerCallsAskTheRepositoryForOpenCallsOfTheRightScope() {
        when(postRepository.findFeed(any(), any())).thenReturn(Page.empty());

        service.getOpenPlayerCalls(null, true, PostSport.FOOTBALL, 100);

        org.mockito.ArgumentCaptor<PostRepository.FeedQuery> query = org.mockito.ArgumentCaptor.forClass(PostRepository.FeedQuery.class);
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> page =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(postRepository).findFeed(query.capture(), page.capture());
        assertThat(query.getValue().playerCalls()).isEqualTo(new PostRepository.PlayerCallFilter(true, true));
        assertThat(query.getValue().sport()).isEqualTo(PostSport.FOOTBALL);
        assertThat(page.getValue().getPageSize()).isEqualTo(24);
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
                .content("Cảm ơn @Bảo, @Hà, @Tôi, @Người chặn hôm nay!")
                .visibility(PostVisibility.FRIENDS)
                .mentions(List.of(new Mention(friend, "Bảo"), new Mention(stranger, "Hà"), new Mention(author, "Tôi"),
                        new Mention(blocked, "Người chặn"), new Mention(friend, "Bảo"),
                        new Mention(UUID.randomUUID(), "Không có trong bài")))
                .build());

        assertThat(response.getMentions()).extracting(Mention::userId).containsExactly(friend, stranger);
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
        existing.setMentions(List.of(new Mention(first, "An")));
        when(postRepository.findById(existing.getPostId())).thenReturn(Optional.of(existing));
        savesReturnTheirArgument();

        service.updatePost(existing.getPostId(), SavePostCommand.builder()
                .actorId(author).content("Cập nhật đội hình: @An và @Bình").visibility(PostVisibility.PUBLIC)
                .mentions(List.of(new Mention(first, "An"), new Mention(second, "Bình"))).build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_MENTIONED), eq(second), any(), any(), anyMap(), anyString());
        verify(eventPublisher, never()).publish(anyString(), eq(first), any(), any(), anyMap(), any());
    }

    @Test
    @DisplayName("nhắc tên trong bình luận: không báo trùng người vừa nhận thông báo bình luận")
    void commentMentionsNotifyOnlyPeopleNotAlreadyTold() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID commenter = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(commentRepository.save(any(PostComment.class))).thenAnswer(call -> {
            PostComment saved = call.getArgument(0);
            saved.setCommentId(UUID.randomUUID());
            return saved;
        });

        service.createComment(target.getPostId(), SaveCommentCommand.builder()
                .actorId(commenter).content("@Chủ bài ơi, rủ thêm @Khoa nhé #keotoi")
                .mentions(List.of(new Mention(author, "Chủ bài"), new Mention(teammate, "Khoa"))).build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_COMMENTED), eq(author), eq(commenter), any(), anyMap(), isNull());
        verify(eventPublisher).publish(eq(SocialEventPublisher.COMMENT_MENTIONED), eq(teammate), eq(commenter), any(), anyMap(),
                anyString());
        verify(eventPublisher, never()).publish(eq(SocialEventPublisher.COMMENT_MENTIONED), eq(author), any(), any(), anyMap(), any());
    }

    @Test
    void editingACommentOnlyNotifiesNewlyMentionedPeople() throws ServiceException {
        UUID commenter = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Post target = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        PostComment comment = PostComment.builder().commentId(UUID.randomUUID()).postId(target.getPostId())
                .authorId(commenter).content("@An").status(PostStatus.PUBLISHED)
                .mentions(List.of(new Mention(first, "An"))).build();
        when(commentRepository.findById(comment.getCommentId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(PostComment.class))).thenAnswer(call -> call.getArgument(0));
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));

        service.updateComment(comment.getCommentId(), commenter, "@An và @Bình",
                List.of(new Mention(first, "An"), new Mention(second, "Bình")));

        verify(eventPublisher).publish(eq(SocialEventPublisher.COMMENT_MENTIONED), eq(second), any(), any(), anyMap(), anyString());
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
