package iuh.fit.socialservice.infrastructure.accountdeletion;

import iuh.fit.socialservice.infrastructure.external.client.StorageClient;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostAttachmentDoc;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaConversationRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFriendshipRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaUserBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContentEraserTest {
    @Mock private MongoTemplate mongo;
    @Mock private JpaFriendshipRepository friendships;
    @Mock private JpaFollowRepository follows;
    @Mock private JpaUserBlockRepository blocks;
    @Mock private JpaConversationRepository conversations;
    @Mock private StorageClient storageClient;
    @InjectMocks private UserContentEraser eraser;

    @Test
    void removesPostsWithTheirThreadsRelationshipsAndGroupSeatsButKeepsGoingWhenAFileFails() {
        UUID userId = UUID.randomUUID();
        PostEntity post = PostEntity.builder().postId("post-1").authorId(userId.toString())
                .attachments(List.of(PostAttachmentDoc.builder().storageKey("social-posts/a.jpg").build(),
                        PostAttachmentDoc.builder().storageKey("social-posts/b.jpg").build()))
                .build();
        when(mongo.find(any(Query.class), eq(PostEntity.class))).thenReturn(List.of(post));
        when(mongo.find(any(Query.class), eq(PostCommentEntity.class))).thenReturn(List.of());
        doThrow(new RuntimeException("storage down")).when(storageClient).delete("social-posts/a.jpg");

        eraser.erase(userId);

        ArgumentCaptor<Query> comments = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(comments.capture(), eq(PostCommentEntity.class));
        // Bình luận của người khác dưới bài của người bị xóa cũng đi theo bài.
        assertThat(comments.getValue().getQueryObject().toJson()).contains("post-1").contains(userId.toString());
        verify(mongo).remove(any(Query.class), eq(PostReactionEntity.class));
        verify(mongo).remove(any(Query.class), eq(PostShareEntity.class));
        verify(mongo).remove(any(Query.class), eq(PostEntity.class));
        verify(friendships).deleteAllOf(userId);
        verify(follows).deleteAllOf(userId);
        verify(blocks).deleteAllOf(userId);
        verify(conversations).leaveGroupConversations(eq(userId), any());
        verify(storageClient).delete("social-posts/b.jpg");
    }
}
