package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostShareRepository;
import iuh.fit.socialservice.domain.entity.PostShare;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostShareEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostShareRepositoryImpl implements PostShareRepository {
    private final MongoPostShareRepository mongoRepository;
    private final PostShareEntityMapper mapper;

    @Override
    public PostShare save(PostShare share) {
        PostShareEntity entity = mapper.toEntity(share);
        if (entity.getShareId() == null) entity.setShareId(UUID.randomUUID().toString());
        return mapper.toDomain(mongoRepository.save(entity));
    }

    @Override
    public long countByPostId(UUID postId) {
        return mongoRepository.countByPostId(postId.toString());
    }
}
