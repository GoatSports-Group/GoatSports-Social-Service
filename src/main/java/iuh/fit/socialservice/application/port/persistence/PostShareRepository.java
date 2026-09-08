package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.PostShare;

import java.util.UUID;

public interface PostShareRepository {
    PostShare save(PostShare share);

    long countByPostId(UUID postId);
}
