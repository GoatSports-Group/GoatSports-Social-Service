package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.domain.enums.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private SocialPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocialPostServiceImpl(
                postRepository,
                commentRepository,
                reactionRepository,
                shareRepository,
                friendshipRepository,
                userBlockRepository
        );
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
}
