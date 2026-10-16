package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostReactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SocialStatsRepositoryImpl implements SocialStatsRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<PostFact> postsPublished(LocalDateTime from, LocalDateTime to) {
        return posts(Criteria.where("status").is(PostStatus.PUBLISHED).and("publishedAt").gte(from).lt(to));
    }

    @Override
    public List<PostFact> postsById(Collection<String> postIds) {
        return posts(Criteria.where("_id").in(postIds).and("status").is(PostStatus.PUBLISHED));
    }

    private List<PostFact> posts(Criteria criteria) {
        Query query = Query.query(criteria);
        query.fields().include("authorId", "publishedAt", "sport", "sharedPostId", "tags", "tagKeys",
                "playerCall.filled", "content");
        return mongoTemplate.find(query, PostEntity.class).stream()
                .map(post -> new PostFact(post.getPostId(), post.getAuthorId(), post.getPublishedAt(), post.getSport(),
                        post.getSharedPostId(), post.getTags() == null ? List.of() : post.getTags(),
                        post.getTagKeys() == null ? List.of() : post.getTagKeys(),
                        post.getPlayerCall() == null ? null : post.getPlayerCall().isFilled(), post.getContent()))
                .toList();
    }

    @Override
    public List<Interaction> interactions(LocalDateTime from, LocalDateTime to) {
        List<Interaction> interactions = new ArrayList<>();
        Query reactions = Query.query(Criteria.where("reactedAt").gte(from).lt(to));
        reactions.fields().include("userId", "postId", "type", "reactedAt");
        mongoTemplate.find(reactions, PostReactionEntity.class).forEach(reaction -> interactions.add(new Interaction(
                reaction.getType() == ReactionType.SAVE ? InteractionKind.SAVE : InteractionKind.LIKE,
                reaction.getUserId(), reaction.getPostId(), reaction.getReactedAt())));
        Query comments = Query.query(Criteria.where("status").is(PostStatus.PUBLISHED).and("createdAt").gte(from).lt(to));
        comments.fields().include("authorId", "postId", "createdAt");
        mongoTemplate.find(comments, PostCommentEntity.class).forEach(comment -> interactions.add(new Interaction(
                InteractionKind.COMMENT, comment.getAuthorId(), comment.getPostId(), comment.getCreatedAt())));
        return interactions;
    }
}
