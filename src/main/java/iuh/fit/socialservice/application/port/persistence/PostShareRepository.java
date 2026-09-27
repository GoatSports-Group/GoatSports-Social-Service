package iuh.fit.socialservice.application.port.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Luot chia se kieu cu (truoc khi chia se thanh bai viet), chi con de dem. Chia se moi la mot
 * {@code Post} co {@code sharedPostId}, dem bang {@link PostRepository#countPublishedShares}.
 */
public interface PostShareRepository {
    Map<UUID, Long> countByPostIds(Collection<UUID> postIds);
}
