package iuh.fit.socialservice.infrastructure.persistence.repository.mongo;

import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoPostShareRepository extends MongoRepository<PostShareEntity, String> {
    long countByPostId(String postId);
}
