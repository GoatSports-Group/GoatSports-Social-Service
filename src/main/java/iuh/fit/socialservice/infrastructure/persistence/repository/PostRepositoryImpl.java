package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {
    private final MongoPostRepository mongoPostRepository;
    private final MongoTemplate mongoTemplate;
    private final PostEntityMapper postEntityMapper;

    @Override
    public Post save(Post post) {
        PostEntity entity = postEntityMapper.toEntity(post);
        if (entity.getPostId() == null) entity.setPostId(UUID.randomUUID().toString());
        if (entity.getAttachments() != null) {
            entity.getAttachments().forEach(attachment -> {
                if (attachment.getAttachmentId() == null) {
                    attachment.setAttachmentId(UUID.randomUUID().toString());
                }
            });
        }
        return postEntityMapper.toDomain(mongoPostRepository.save(entity));
    }

    @Override
    public Optional<Post> findById(UUID postId) {
        return mongoPostRepository.findById(postId.toString()).map(postEntityMapper::toDomain);
    }

    @Override
    public Page<Post> findFeed(
            UUID viewerId,
            Set<UUID> friendIds,
            Set<UUID> followingIds,
            Set<UUID> blockedUserIds,
            boolean followingOnly,
            Pageable pageable
    ) {
        Criteria criteria = feedCriteria(viewerId, friendIds, followingIds, blockedUserIds, followingOnly);
        long total = mongoTemplate.count(new Query(criteria), PostEntity.class);
        Query query = new Query(criteria).with(pageable);
        List<Post> posts = mongoTemplate.find(query, PostEntity.class).stream()
                .map(postEntityMapper::toDomain)
                .toList();
        return new PageImpl<>(posts, pageable, total);
    }

    private Criteria feedCriteria(
            UUID viewerId,
            Set<UUID> friendIds,
            Set<UUID> followingIds,
            Set<UUID> blockedUserIds,
            boolean followingOnly
    ) {
        Criteria visibility;
        if (viewerId == null) {
            visibility = Criteria.where("visibility").is(PostVisibility.PUBLIC);
        } else {
            Criteria publicPosts = Criteria.where("visibility").is(PostVisibility.PUBLIC);
            Criteria ownPosts = Criteria.where("authorId").is(viewerId.toString());
            Criteria friendPosts = new Criteria().andOperator(
                    Criteria.where("visibility").is(PostVisibility.FRIENDS),
                    Criteria.where("authorId").in(friendIds.stream().map(UUID::toString).toList())
            );
            visibility = new Criteria().orOperator(publicPosts, ownPosts, friendPosts);
        }

        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("status").is(PostStatus.PUBLISHED));
        criteria.add(visibility);
        if (!blockedUserIds.isEmpty()) {
            criteria.add(Criteria.where("authorId").nin(blockedUserIds.stream().map(UUID::toString).toList()));
        }
        if (followingOnly && viewerId != null) {
            Set<UUID> authors = new HashSet<>(friendIds);
            authors.addAll(followingIds);
            authors.add(viewerId);
            criteria.add(Criteria.where("authorId").in(authors.stream().map(UUID::toString).toList()));
        }
        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }
}
