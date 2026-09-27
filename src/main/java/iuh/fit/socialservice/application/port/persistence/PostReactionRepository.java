package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.ReactionType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PostReactionRepository {
    PostReaction save(PostReaction reaction);

    Optional<PostReaction> find(UUID postId, UUID userId, ReactionType type);

    void delete(UUID postId, UUID userId, ReactionType type);

    /** postId → so luot; bai khong co luot nao thi khong co trong map. */
    Map<UUID, Long> countByPostIds(Collection<UUID> postIds, ReactionType type);

    /** Moi reaction (moi loai) cua mot nguoi tren cac bai cho truoc. */
    List<PostReaction> findByUserAndPostIds(UUID userId, Collection<UUID> postIds);
}
