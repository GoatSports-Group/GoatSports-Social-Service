package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PostCommentRepository {
    PostComment save(PostComment comment);

    Optional<PostComment> findById(UUID commentId);

    Page<PostComment> findPublishedByPostId(UUID postId, Pageable pageable);

    /** postId → so binh luan dang hien. */
    Map<UUID, Long> countPublishedByPostIds(Collection<UUID> postIds);
}
