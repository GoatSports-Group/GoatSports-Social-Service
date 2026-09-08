package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostCommentEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.PostCommentEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.mongo.MongoPostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostCommentRepositoryImpl implements PostCommentRepository {
    private final MongoPostCommentRepository mongoRepository;
    private final PostCommentEntityMapper mapper;

    @Override
    public PostComment save(PostComment comment) {
        PostCommentEntity entity = mapper.toEntity(comment);
        if (entity.getCommentId() == null) entity.setCommentId(UUID.randomUUID().toString());
        return mapper.toDomain(mongoRepository.save(entity));
    }

    @Override
    public Optional<PostComment> findById(UUID commentId) {
        return mongoRepository.findById(commentId.toString()).map(mapper::toDomain);
    }

    @Override
    public Page<PostComment> findPublishedByPostId(UUID postId, Pageable pageable) {
        return mongoRepository.findByPostIdAndStatusOrderByCreatedAtAsc(
                postId.toString(),
                PostStatus.PUBLISHED,
                pageable
        ).map(mapper::toDomain);
    }

    @Override
    public long countPublishedByPostId(UUID postId) {
        return mongoRepository.countByPostIdAndStatus(postId.toString(), PostStatus.PUBLISHED);
    }
}
