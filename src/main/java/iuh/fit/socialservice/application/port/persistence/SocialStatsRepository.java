package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.enums.PostSport;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Dữ kiện thô cho thống kê cộng đồng của admin: bài đang hiển thị và tương tác trong một khoảng thời gian. */
public interface SocialStatsRepository {

    /** Bài (kể cả bài chia sẻ) đang hiển thị, đăng trong [from, to). */
    List<PostFact> postsPublished(LocalDateTime from, LocalDateTime to);

    /** Bài theo mã, để biết môn / nội dung của bài cũ được tương tác trong kỳ. */
    List<PostFact> postsById(Collection<String> postIds);

    /** Lượt thích, lưu và bình luận đang hiển thị trong [from, to). */
    List<Interaction> interactions(LocalDateTime from, LocalDateTime to);

    /** {@code playerCallFilled}: null khi bài không phải tin tìm người chơi. */
    record PostFact(String postId, String authorId, LocalDateTime publishedAt, PostSport sport, String sharedPostId,
                    List<String> tags, List<String> tagKeys, Boolean playerCallFilled, String content) { }

    enum InteractionKind { LIKE, SAVE, COMMENT }

    record Interaction(InteractionKind kind, String userId, String postId, LocalDateTime at) { }
}
