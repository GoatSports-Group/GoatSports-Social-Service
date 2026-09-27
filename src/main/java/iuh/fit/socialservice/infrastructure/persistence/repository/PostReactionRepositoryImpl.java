package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostReactionEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostReactionRepositoryImpl implements PostReactionRepository {
    private final MongoPostReactionRepository mongoRepository;
    private final PostReactionEntityMapper mapper;

    @Override
    public PostReaction save(PostReaction reaction) {
        PostReactionEntity entity = mapper.toEntity(reaction);
        if (entity.getReactionId() == null) entity.setReactionId(UUID.randomUUID().toString());
        return mapper.toDomain(mongoRepository.save(entity));
    }

    @Override
    public Optional<PostReaction> find(UUID postId, UUID userId, ReactionType type) {
        return mongoRepository.findByPostIdAndUserIdAndType(
                postId.toString(), userId.toString(), type
        ).map(mapper::toDomain);
    }

    @Override
    public long count(UUID postId, ReactionType type) {
        return mongoRepository.countByPostIdAndType(postId.toString(), type);
    }

    @Override
    public void delete(UUID postId, UUID userId, ReactionType type) {
        mongoRepository.deleteByPostIdAndUserIdAndType(postId.toString(), userId.toString(), type);
    }

    @Override
    public Page<PostReaction> findByUser(UUID userId, ReactionType type, Pageable pageable) {
        return mongoRepository.findByUserIdAndTypeOrderByReactedAtDesc(userId.toString(), type, pageable)
                .map(mapper::toDomain);
    }
}
