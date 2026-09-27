package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

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
    public List<Post> findAllById(Collection<UUID> postIds) {
        if (postIds.isEmpty()) return List.of();
        return StreamSupport.stream(mongoPostRepository.findAllById(strings(postIds)).spliterator(), false)
                .map(postEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Post> findFeed(FeedQuery feed, Pageable pageable) {
        Criteria criteria = feedCriteria(feed);
        long total = mongoTemplate.count(new Query(criteria), PostEntity.class);
        Query query = new Query(criteria).with(pageable);
        List<Post> posts = mongoTemplate.find(query, PostEntity.class).stream()
                .map(postEntityMapper::toDomain)
                .toList();
        return new PageImpl<>(posts, pageable, total);
    }

    @Override
    public List<AuthorActivity> findActiveAuthors(
            LocalDateTime since,
            Collection<UUID> excludedAuthorIds,
            int limit
    ) {
        // Chi dem bai cong khai: goi y khong duoc de lo hoat dong cua bai chi ban be xem.
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(new Criteria().andOperator(
                        Criteria.where("status").is(PostStatus.PUBLISHED),
                        Criteria.where("visibility").is(PostVisibility.PUBLIC),
                        Criteria.where("publishedAt").gte(since),
                        Criteria.where("authorId").nin(strings(excludedAuthorIds))
                )),
                Aggregation.group("authorId").count().as("postCount"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "postCount")),
                Aggregation.limit(limit)
        );
        return mongoTemplate.aggregate(aggregation, PostEntity.class, Document.class).getMappedResults().stream()
                .map(row -> new AuthorActivity(
                        UUID.fromString(row.getString("_id")),
                        ((Number) row.get("postCount")).longValue()))
                .toList();
    }

    private Criteria feedCriteria(FeedQuery feed) {
        UUID viewerId = feed.viewerId();
        Criteria visibility;
        if (viewerId == null) {
            visibility = Criteria.where("visibility").is(PostVisibility.PUBLIC);
        } else {
            Criteria publicPosts = Criteria.where("visibility").is(PostVisibility.PUBLIC);
            Criteria ownPosts = Criteria.where("authorId").is(viewerId.toString());
            Criteria friendPosts = new Criteria().andOperator(
                    Criteria.where("visibility").is(PostVisibility.FRIENDS),
                    Criteria.where("authorId").in(strings(feed.friendIds()))
            );
            visibility = new Criteria().orOperator(publicPosts, ownPosts, friendPosts);
        }

        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("status").is(PostStatus.PUBLISHED));
        criteria.add(visibility);
        if (!feed.blockedUserIds().isEmpty()) {
            criteria.add(Criteria.where("authorId").nin(strings(feed.blockedUserIds())));
        }
        if (feed.followingOnly() && viewerId != null) {
            Set<UUID> authors = new HashSet<>(feed.friendIds());
            authors.addAll(feed.followingIds());
            authors.add(viewerId);
            criteria.add(Criteria.where("authorId").in(strings(authors)));
        }
        if (feed.authorId() != null) {
            criteria.add(Criteria.where("authorId").is(feed.authorId().toString()));
        }
        if (feed.sport() != null) {
            criteria.add(Criteria.where("sport").is(feed.sport()));
        }
        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }

    private static List<String> strings(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).toList();
    }
}
