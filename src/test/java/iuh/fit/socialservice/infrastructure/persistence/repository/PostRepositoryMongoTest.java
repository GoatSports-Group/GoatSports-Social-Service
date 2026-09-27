package iuh.fit.socialservice.infrastructure.persistence.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.migration.ShareRowDedupeMigration;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostReactionEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostReactionRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Chay that cac aggregation ($lookup, $facet, $group) tren MongoDB cuc bo — mock khong bat duoc sai
 * ten truong hay sai kieu enum trong pipeline. Tu bo qua khi khong co Mongo o {@code SOCIAL_IT_MONGO_URI}
 * (mac dinh mongodb://localhost:27017). Moi lan chay dung mot database rieng va xoa sau khi xong.
 */
class PostRepositoryMongoTest {
    private static MongoClient client;
    private static MongoTemplate template;
    private static String database;

    private PostRepositoryImpl posts;
    private PostReactionRepositoryImpl reactions;
    private PostShareRepositoryImpl shares;

    private final UUID viewer = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();
    private final UUID blocked = UUID.randomUUID();

    @BeforeAll
    static void connect() {
        String uri = System.getenv().getOrDefault("SOCIAL_IT_MONGO_URI",
                "mongodb://localhost:27017/?serverSelectionTimeoutMS=1500&connectTimeoutMS=1500");
        try {
            client = MongoClients.create(uri);
            database = "goat_social_it_" + UUID.randomUUID().toString().substring(0, 8);
            client.getDatabase(database).runCommand(new Document("ping", 1));
            template = new MongoTemplate(client, database);
        } catch (RuntimeException unavailable) {
            assumeTrue(false, "MongoDB không sẵn sàng: " + unavailable.getMessage());
        }
    }

    @AfterAll
    static void dropDatabase() {
        if (client == null) return;
        if (template != null) client.getDatabase(database).drop();
        client.close();
    }

    @BeforeEach
    void wire() {
        MongoRepositoryFactory factory = new MongoRepositoryFactory(template);
        template.getDb().drop();
        posts = new PostRepositoryImpl(factory.getRepository(MongoPostRepository.class), template, new PostEntityMapper());
        reactions = new PostReactionRepositoryImpl(factory.getRepository(MongoPostReactionRepository.class), template,
                new PostReactionEntityMapper());
        shares = new PostShareRepositoryImpl(template);
    }

    private Post publish(UUID author, PostVisibility visibility, String content, int minutesAgo) {
        LocalDateTime at = LocalDateTime.now().minusMinutes(minutesAgo);
        return posts.save(Post.builder()
                .authorId(author).content(content).visibility(visibility).status(PostStatus.PUBLISHED)
                .createdAt(at).updatedAt(at).publishedAt(at)
                .tags(iuh.fit.socialservice.domain.service.PostTags.extract(content))
                .build());
    }

    private void save(Post post, int minutesAgo) {
        reactions.save(PostReaction.builder().postId(post.getPostId()).userId(viewer).type(ReactionType.SAVE)
                .reactedAt(LocalDateTime.now().minusMinutes(minutesAgo)).build());
    }

    private PostRepository.FeedQuery visibility(String tag) {
        return new PostRepository.FeedQuery(viewer, Set.of(), Set.of(), Set.of(blocked), false, null, null, tag);
    }

    @Test
    void savedPostsFilterBeforePagingSoEveryPageIsFull() {
        Post older = publish(stranger, PostVisibility.PUBLIC, "cũ", 50);
        Post friendsOnly = publish(stranger, PostVisibility.FRIENDS, "chỉ bạn bè", 40);
        Post byBlocked = publish(blocked, PostVisibility.PUBLIC, "người bị chặn", 30);
        Post newest = publish(stranger, PostVisibility.PUBLIC, "mới", 20);
        save(older, 10);
        save(friendsOnly, 8);
        save(byBlocked, 6);
        save(newest, 4);

        Page<Post> first = posts.findSaved(visibility(null), PageRequest.of(0, 1));
        Page<Post> second = posts.findSaved(visibility(null), PageRequest.of(1, 1));

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).extracting(Post::getPostId).containsExactly(newest.getPostId());
        assertThat(second.getContent()).extracting(Post::getPostId).containsExactly(older.getPostId());
        assertThat(first.getContent().get(0).getVisibility()).isEqualTo(PostVisibility.PUBLIC);
    }

    @Test
    void shareCountsFollowTheShareStatusAndIncludeLegacyRows() {
        Post original = publish(stranger, PostVisibility.PUBLIC, "gốc", 30);
        for (PostStatus status : List.of(PostStatus.PUBLISHED, PostStatus.PUBLISHED, PostStatus.REMOVED)) {
            Post share = publish(viewer, PostVisibility.PUBLIC, null, 10);
            share.setSharedPostId(original.getPostId());
            share.setStatus(status);
            posts.save(share);
        }
        template.save(PostShareEntity.builder().shareId(UUID.randomUUID().toString())
                .postId(original.getPostId().toString()).userId(viewer.toString()).sharedAt(LocalDateTime.now()).build());

        assertThat(posts.countPublishedShares(List.of(original.getPostId()))).containsEntry(original.getPostId(), 2L);
        assertThat(shares.countByPostIds(List.of(original.getPostId()))).containsEntry(original.getPostId(), 1L);
    }

    @Test
    void reactionCountsAndViewerReactionsComeBackInOneQueryEach() {
        Post a = publish(stranger, PostVisibility.PUBLIC, "a", 5);
        Post b = publish(stranger, PostVisibility.PUBLIC, "b", 5);
        for (UUID user : List.of(viewer, stranger)) {
            reactions.save(PostReaction.builder().postId(a.getPostId()).userId(user).type(ReactionType.LIKE)
                    .reactedAt(LocalDateTime.now()).build());
        }
        save(b, 1);

        assertThat(reactions.countByPostIds(List.of(a.getPostId(), b.getPostId()), ReactionType.LIKE))
                .containsExactlyEntriesOf(java.util.Map.of(a.getPostId(), 2L));
        assertThat(reactions.findByUserAndPostIds(viewer, List.of(a.getPostId(), b.getPostId())))
                .extracting(PostReaction::getType)
                .containsExactlyInAnyOrder(ReactionType.LIKE, ReactionType.SAVE);
    }

    @Test
    void tagFilterAndTrendingTagsOnlyCountPublicPosts() {
        publish(stranger, PostVisibility.PUBLIC, "#bongda #caulong", 5);
        publish(stranger, PostVisibility.PUBLIC, "#bongda tối nay", 4);
        publish(stranger, PostVisibility.FRIENDS, "#bongda riêng tư", 3);

        assertThat(posts.findTrendingTags(LocalDateTime.now().minusDays(7), 5))
                .containsExactly(new PostRepository.TagCount("bongda", 2), new PostRepository.TagCount("caulong", 1));
        Page<Post> tagged = posts.findFeed(visibility("caulong"), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt")));
        assertThat(tagged.getTotalElements()).isEqualTo(1);
    }

    @Test
    void emptyIdListSkipsTheQuery() {
        assertThat(posts.countPublishedShares(List.of())).isEmpty();
    }

    @Test
    void cleanupDropsShareRowsAlreadyRepresentedBySharePosts() {
        Post original = publish(stranger, PostVisibility.PUBLIC, "gốc", 30);
        Post share = publish(viewer, PostVisibility.PUBLIC, null, 10);
        share.setSharedPostId(original.getPostId());
        posts.save(share);
        for (UUID user : List.of(viewer, stranger)) {
            template.save(PostShareEntity.builder().shareId(UUID.randomUUID().toString())
                    .postId(original.getPostId().toString()).userId(user.toString()).sharedAt(LocalDateTime.now()).build());
        }
        ShareRowDedupeMigration cleanup = new ShareRowDedupeMigration(template);

        // viewer's row duplicates their share post; stranger's row is a genuine old-style share.
        assertThat(cleanup.removeRowsCoveredBySharePosts()).isEqualTo(1);
        assertThat(cleanup.removeRowsCoveredBySharePosts()).as("idempotent").isZero();
        assertThat(shares.countByPostIds(List.of(original.getPostId()))).containsEntry(original.getPostId(), 1L);
    }
}
