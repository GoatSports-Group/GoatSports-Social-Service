package iuh.fit.socialservice.infrastructure.persistence.repository.mongo;

import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoPostCommentRepository extends MongoRepository<PostCommentEntity, String> {
    Page<PostCommentEntity> findByPostIdAndStatusOrderByCreatedAtAsc(
            String postId,
            PostStatus status,
            Pageable pageable
    );

}
