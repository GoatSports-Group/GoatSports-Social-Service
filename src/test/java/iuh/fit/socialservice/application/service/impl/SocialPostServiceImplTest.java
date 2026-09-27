package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.application.port.persistence.FollowRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.domain.entity.UserBlock;
import iuh.fit.socialservice.domain.entity.PostReaction;
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

import java.time.LocalDateTime;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialPostServiceImplTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository commentRepository;
    @Mock
    private PostReactionRepository reactionRepository;
    @Mock
    private PostShareRepository shareRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private iuh.fit.socialservice.application.port.external.StorageKeyPromoter storageKeyPromoter;
    @Mock
    private SocialEventPublisher eventPublisher;

    private SocialPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocialPostServiceImpl(
                postRepository,
                commentRepository,
                reactionRepository,
                shareRepository,
                friendshipRepository,
                userBlockRepository,
                followRepository,
                storageKeyPromoter,
                eventPublisher
        );
        // Mac dinh: storage tra lai dung khoa da gui, de test cu khong phu thuoc vao promote.
        org.mockito.Mockito.lenient()
                .when(storageKeyPromoter.promote(org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createPostNormalizesContentAndPersistsAttachments() throws ServiceException {
        UUID actorId = UUID.randomUUID();
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setPostId(UUID.randomUUID());
            return post;
        });

        service.createPost(SavePostCommand.builder()
                .actorId(actorId)
                .content("  Một buổi tập tuyệt vời  ")
                .visibility(PostVisibility.FRIENDS)
                .attachments(List.of(SavePostCommand.AttachmentItem.builder()
                        .storageKey("temp/social-posts/photo.jpg")
                        .type(AttachmentType.IMAGE)
                        .displayOrder(0)
                        .build()))
                .build());

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getAuthorId()).isEqualTo(actorId);
        assertThat(saved.getContent()).isEqualTo("Một buổi tập tuyệt vời");
        assertThat(saved.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(saved.getAttachments()).singleElement().satisfies(attachment -> {
            assertThat(attachment.getStorageKey()).isEqualTo("temp/social-posts/photo.jpg");
            assertThat(attachment.getType()).isEqualTo(AttachmentType.IMAGE);
        });
    }

    @Test
    void createPostRejectsAnEmptyPost() {
        SavePostCommand command = SavePostCommand.builder()
                .actorId(UUID.randomUUID())
                .content("   ")
                .visibility(PostVisibility.PUBLIC)
                .attachments(List.of())
                .build();

        assertThatThrownBy(() -> service.createPost(command))
                .isInstanceOf(ServiceException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void likePostIsIdempotent() throws ServiceException {
        UUID postId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Post post = Post.builder()
                .postId(postId)
                .authorId(UUID.randomUUID())
                .content("Post")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .attachments(List.of())
                .build();
        PostReaction existing = PostReaction.builder()
                .reactionId(UUID.randomUUID())
                .postId(postId)
                .userId(actorId)
                .type(ReactionType.LIKE)
                .build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(reactionRepository.find(postId, actorId, ReactionType.LIKE)).thenReturn(Optional.of(existing));

        service.likePost(postId, actorId);

        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("bài viết lưu khóa vĩnh viễn, không lưu khóa temp/")
    void createPostStoresPromotedStorageKey() throws ServiceException {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setPostId(UUID.randomUUID());
            return post;
        });
        org.mockito.Mockito.when(storageKeyPromoter.promote(org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of("social-posts/photo.jpg"));

        service.createPost(SavePostCommand.builder()
                .actorId(UUID.randomUUID())
                .content("Có ảnh")
                .visibility(PostVisibility.PUBLIC)
                .attachments(List.of(SavePostCommand.AttachmentItem.builder()
                        .storageKey("temp/social-posts/photo.jpg")
                        .type(AttachmentType.IMAGE)
                        .displayOrder(0)
                        .build()))
                .build());

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getAttachments()).singleElement().satisfies(attachment ->
                assertThat(attachment.getStorageKey()).isEqualTo("social-posts/photo.jpg"));
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
                .attachments(List.of())
                .build();
    }

    private void savesReturnTheirArgument() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            if (saved.getPostId() == null) saved.setPostId(UUID.randomUUID());
            return saved;
        });
    }

    @Test
    @DisplayName("người bị chặn không thích được bài công khai của người đã chặn mình")
    void blockedViewerCannotLikeAPublicPost() {
        UUID author = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(userBlockRepository.findRelatedToUser(viewer)).thenReturn(List.of(
                UserBlock.builder().blockerId(author).blockedUserId(viewer).build()));

        assertThatThrownBy(() -> service.likePost(target.getPostId(), viewer))
                .isInstanceOf(ServiceException.class);
        verify(reactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(anyString(), any(), any(), any(), anyMap());
    }

    @Test
    void likeNotifiesTheAuthorOnceButNeverTheActorThemself() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID fan = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(reactionRepository.find(target.getPostId(), fan, ReactionType.LIKE)).thenReturn(Optional.empty());

        service.likePost(target.getPostId(), fan);
        service.likePost(target.getPostId(), author);

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_LIKED), eq(author), eq(fan),
                eq(target.getPostId()), anyMap());
        verify(eventPublisher, never()).publish(anyString(), eq(author), eq(author), any(), anyMap());
    }

    @Test
    void replyNotifiesBothThePostAuthorAndTheParentCommentAuthor() throws ServiceException {
        UUID author = UUID.randomUUID();
        UUID firstCommenter = UUID.randomUUID();
        UUID replier = UUID.randomUUID();
        Post target = post(author, PostVisibility.PUBLIC);
        PostComment parent = PostComment.builder()
                .commentId(UUID.randomUUID())
                .postId(target.getPostId())
                .authorId(firstCommenter)
                .status(PostStatus.PUBLISHED)
                .build();
        when(postRepository.findById(target.getPostId())).thenReturn(Optional.of(target));
        when(commentRepository.findById(parent.getCommentId())).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(PostComment.class))).thenAnswer(call -> call.getArgument(0));

        service.createComment(target.getPostId(), SaveCommentCommand.builder()
                .actorId(replier)
                .parentCommentId(parent.getCommentId())
                .content("Mình vào sân được lúc 19h")
                .build());

        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_COMMENTED), eq(author), eq(replier), any(), anyMap());
        verify(eventPublisher).publish(eq(SocialEventPublisher.COMMENT_REPLIED), eq(firstCommenter), eq(replier), any(), anyMap());
    }

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
        savesReturnTheirArgument();

        PostResponseCommand share = service.sharePost(firstShare.getPostId(), SharePostCommand.builder()
                .actorId(sharer)
                .caption("  Ai rảnh vào đội mình  ")
                .build());

        assertThat(share.getSharedPostId()).isEqualTo(original.getPostId());
        assertThat(share.getSport()).isEqualTo(PostSport.BADMINTON);
        assertThat(share.getContent()).isEqualTo("Ai rảnh vào đội mình");
        assertThat(share.getSharedPost().getPostId()).isEqualTo(original.getPostId());
        verify(eventPublisher).publish(eq(SocialEventPublisher.POST_SHARED), eq(author), eq(sharer),
                eq(original.getPostId()), anyMap());
    }

    @Test
    void onlyPublicPostsCanBeShared() {
        UUID author = UUID.randomUUID();
        Post friendsOnly = post(author, PostVisibility.FRIENDS);
        when(postRepository.findById(friendsOnly.getPostId())).thenReturn(Optional.of(friendsOnly));

        assertThatThrownBy(() -> service.sharePost(friendsOnly.getPostId(), SharePostCommand.builder()
                .actorId(author)
                .build()))
                .isInstanceOf(ServiceException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("bài gốc bị ẩn thì bài chia sẻ không làm lộ nội dung")
    void hiddenOriginalIsNotEmbeddedInAShare() throws ServiceException {
        UUID viewer = UUID.randomUUID();
        Post original = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        original.setStatus(PostStatus.HIDDEN);
        Post share = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        share.setSharedPostId(original.getPostId());
        when(postRepository.findById(share.getPostId())).thenReturn(Optional.of(share));
        when(postRepository.findById(original.getPostId())).thenReturn(Optional.of(original));

        PostResponseCommand response = service.getPost(share.getPostId(), viewer);

        assertThat(response.getSharedPost()).isNull();
        assertThat(response.isSharedPostUnavailable()).isTrue();
    }

    @Test
    void savedListSkipsPostsTheViewerCanNoLongerSee() throws ServiceException {
        UUID viewer = UUID.randomUUID();
        Post visible = post(UUID.randomUUID(), PostVisibility.PUBLIC);
        Post nowFriendsOnly = post(UUID.randomUUID(), PostVisibility.FRIENDS);
        PageRequest page = PageRequest.of(0, 10);
        when(reactionRepository.findByUser(viewer, ReactionType.SAVE, page)).thenReturn(new PageImpl<>(List.of(
                PostReaction.builder().postId(nowFriendsOnly.getPostId()).userId(viewer).type(ReactionType.SAVE).build(),
                PostReaction.builder().postId(visible.getPostId()).userId(viewer).type(ReactionType.SAVE).build()
        ), page, 2));
        when(postRepository.findAllById(any())).thenReturn(List.of(visible, nowFriendsOnly));
        lenient().when(reactionRepository.find(any(), any(), any())).thenReturn(Optional.empty());

        List<PostResponseCommand> saved = service.getSavedPosts(viewer, page).getContent();

        assertThat(saved).extracting(PostResponseCommand::getPostId).containsExactly(visible.getPostId());
    }

    @Test
    void snippetFlattensWhitespaceAndCapsTheLength() {
        assertThat(SocialPostServiceImpl.snippet("  a\n\n b  ")).isEqualTo("a b");
        assertThat(SocialPostServiceImpl.snippet("x".repeat(200))).hasSize(80).endsWith("…");
        assertThat(SocialPostServiceImpl.snippet(null)).isEmpty();
    }
}
