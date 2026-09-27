package iuh.fit.socialservice.infrastructure.persistence.repository.mongo;

import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MongoPostReactionRepository extends MongoRepository<PostReactionEntity, String> {
    Optional<PostReactionEntity> findByPostIdAndUserIdAndType(
            String postId,
            String userId,
            ReactionType type
    );

    void deleteByPostIdAndUserIdAndType(String postId, String userId, ReactionType type);

    List<PostReactionEntity> findByUserIdAndPostIdIn(String userId, Collection<String> postIds);
}
