package iuh.fit.socialservice.infrastructure.persistence.repository.mongo;

import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MongoPostReactionRepository extends MongoRepository<PostReactionEntity, String> {
    Optional<PostReactionEntity> findByPostIdAndUserIdAndType(
            String postId,
            String userId,
            ReactionType type
    );

    long countByPostIdAndType(String postId, ReactionType type);

    void deleteByPostIdAndUserIdAndType(String postId, String userId, ReactionType type);

    Page<PostReactionEntity> findByUserIdAndTypeOrderByReactedAtDesc(String userId, ReactionType type, Pageable pageable);
}
