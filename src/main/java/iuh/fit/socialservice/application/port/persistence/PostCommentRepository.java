package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PostCommentRepository {
    PostComment save(PostComment comment);

    Optional<PostComment> findById(UUID commentId);

    Page<PostComment> findPublishedByPostId(UUID postId, Pageable pageable);

    long countPublishedByPostId(UUID postId);
}
