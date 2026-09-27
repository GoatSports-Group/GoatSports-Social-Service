package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostReactionEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostReactionRepositoryImpl implements PostReactionRepository {
    private final MongoPostReactionRepository mongoRepository;
    private final MongoTemplate mongoTemplate;
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
    public void delete(UUID postId, UUID userId, ReactionType type) {
        mongoRepository.deleteByPostIdAndUserIdAndType(postId.toString(), userId.toString(), type);
    }

    @Override
    public Map<UUID, Long> countByPostIds(Collection<UUID> postIds, ReactionType type) {
        return MongoGroupCount.count(mongoTemplate, PostReactionEntity.class, "postId", postIds,
                Criteria.where("type").is(type));
    }

    @Override
    public List<PostReaction> findByUserAndPostIds(UUID userId, Collection<UUID> postIds) {
        if (postIds.isEmpty()) return List.of();
        return mongoRepository.findByUserIdAndPostIdIn(userId.toString(),
                        postIds.stream().map(UUID::toString).toList())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
