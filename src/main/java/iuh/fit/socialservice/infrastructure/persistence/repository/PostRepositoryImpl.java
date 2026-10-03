package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.domain.enums.HighlightKind;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
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
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public boolean existsHighlight(HighlightKind kind, UUID refId, UUID authorId, UUID clubId) {
        Criteria criteria = Criteria.where("status").is(PostStatus.PUBLISHED)
                .and("highlight.kind").is(kind)
                .and("highlight.refId").is(refId.toString());
        criteria = clubId != null ? criteria.and("highlight.clubId").is(clubId.toString())
                : criteria.and("authorId").is(authorId.toString());
        return mongoTemplate.exists(new Query(criteria), PostEntity.class);
    }

    @Override
    public Page<Post> findFeed(FeedQuery feed, Pageable pageable) {
        Criteria criteria = feedCriteria(feed, "");
        long total = mongoTemplate.count(new Query(criteria), PostEntity.class);
        Query query = new Query(criteria).with(pageable);
        List<Post> posts = mongoTemplate.find(query, PostEntity.class).stream()
                .map(postEntityMapper::toDomain)
                .toList();
        return new PageImpl<>(posts, pageable, total);
    }

    @Override
    public Page<Post> findSaved(FeedQuery visibility, Pageable pageable) {
        FacetOperation page = Aggregation.facet(Aggregation.count().as("total")).as("meta")
                .and(Aggregation.skip(pageable.getOffset()), Aggregation.limit(pageable.getPageSize())).as("items");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(visibility.viewerId().toString())
                        .and("type").is(ReactionType.SAVE)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "reactedAt")),
                Aggregation.lookup("posts", "postId", "_id", "post"),
                Aggregation.unwind("post"),
                Aggregation.match(feedCriteria(visibility, "post.")),
                page
        );
        Document result = mongoTemplate.aggregate(aggregation, PostReactionEntity.class, Document.class)
                .getUniqueMappedResult();
        if (result == null) return Page.empty(pageable);
        List<Document> meta = result.getList("meta", Document.class);
        long total = meta.isEmpty() ? 0 : ((Number) meta.get(0).get("total")).longValue();
        List<Post> posts = result.getList("items", Document.class).stream()
                .map(row -> postEntityMapper.toDomain(
                        mongoTemplate.getConverter().read(PostEntity.class, row.get("post", Document.class))))
                .toList();
        return new PageImpl<>(posts, pageable, total);
    }

    @Override
    public Map<UUID, Long> countPublishedShares(Collection<UUID> originalIds) {
        return MongoGroupCount.count(mongoTemplate, PostEntity.class, "sharedPostId", originalIds,
                Criteria.where("status").is(PostStatus.PUBLISHED));
    }

    @Override
    public List<AuthorActivity> findActiveAuthors(
            LocalDateTime since,
            Collection<UUID> excludedAuthorIds,
            Collection<PostSport> sports,
            int limit
    ) {
        // Chi dem bai cong khai: goi y khong duoc de lo hoat dong cua bai chi ban be xem.
        Criteria match = new Criteria().andOperator(
                publicSince(since),
                Criteria.where("authorId").nin(strings(excludedAuthorIds)));
        if (!sports.isEmpty()) match = new Criteria().andOperator(match, Criteria.where("sport").in(sports));
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(match),
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

    @Override
    public List<TagCount> findTrendingTags(LocalDateTime since, int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(publicSince(since).and("tags.0").exists(true)),
                Aggregation.unwind("tags"),
                Aggregation.group("tags").count().as("postCount"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "postCount").and(Sort.by("_id"))),
                Aggregation.limit(limit)
        );
        return mongoTemplate.aggregate(aggregation, PostEntity.class, Document.class).getMappedResults().stream()
                .map(row -> new TagCount(row.getString("_id"), ((Number) row.get("postCount")).longValue()))
                .toList();
    }

    private static Criteria publicSince(LocalDateTime since) {
        return Criteria.where("status").is(PostStatus.PUBLISHED)
                .and("visibility").is(PostVisibility.PUBLIC)
                .and("publishedAt").gte(since);
    }

    /** {@code prefix} = "post." khi tieu chi chay tren bai da $lookup. */
    private Criteria feedCriteria(FeedQuery feed, String prefix) {
        UUID viewerId = feed.viewerId();
        String author = prefix + "authorId";
        String visibilityField = prefix + "visibility";
        Criteria visibility;
        if (viewerId == null) {
            visibility = Criteria.where(visibilityField).is(PostVisibility.PUBLIC);
        } else {
            visibility = new Criteria().orOperator(
                    Criteria.where(visibilityField).is(PostVisibility.PUBLIC),
                    Criteria.where(author).is(viewerId.toString()),
                    new Criteria().andOperator(
                            Criteria.where(visibilityField).is(PostVisibility.FRIENDS),
                            Criteria.where(author).in(strings(feed.friendIds()))));
        }

        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where(prefix + "status").is(PostStatus.PUBLISHED));
        criteria.add(visibility);
        if (!feed.blockedUserIds().isEmpty()) {
            criteria.add(Criteria.where(author).nin(strings(feed.blockedUserIds())));
        }
        if (feed.followingOnly() && viewerId != null) {
            Set<UUID> authors = new HashSet<>(feed.friendIds());
            authors.addAll(feed.followingIds());
            authors.add(viewerId);
            criteria.add(Criteria.where(author).in(strings(authors)));
        }
        if (feed.authorId() != null) criteria.add(Criteria.where(author).is(feed.authorId().toString()));
        if (feed.sport() != null) criteria.add(Criteria.where(prefix + "sport").is(feed.sport()));
        if (feed.sports() != null && !feed.sports().isEmpty()) {
            criteria.add(Criteria.where(prefix + "sport").in(feed.sports()));
        }
        if (feed.tag() != null) criteria.add(Criteria.where(prefix + "tags").is(feed.tag()));
        if (feed.playerCalls() != null) {
            String call = prefix + "playerCall";
            criteria.add(Criteria.where(call).ne(null));
            if (Boolean.TRUE.equals(feed.playerCalls().club())) criteria.add(Criteria.where(call + ".clubId").ne(null));
            if (Boolean.FALSE.equals(feed.playerCalls().club())) criteria.add(Criteria.where(call + ".clubId").is(null));
            if (feed.playerCalls().openOnly()) {
                criteria.add(Criteria.where(call + ".filled").is(false));
                criteria.add(Criteria.where(call + ".playDate").gte(LocalDate.now()));
            }
        }
        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }

    private static List<String> strings(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).toList();
    }
}
