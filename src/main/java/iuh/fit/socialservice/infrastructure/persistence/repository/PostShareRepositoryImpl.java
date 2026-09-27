package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostShareRepositoryImpl implements PostShareRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Map<UUID, Long> countByPostIds(Collection<UUID> postIds) {
        return MongoGroupCount.count(mongoTemplate, PostShareEntity.class, "postId", postIds, null);
    }
}
