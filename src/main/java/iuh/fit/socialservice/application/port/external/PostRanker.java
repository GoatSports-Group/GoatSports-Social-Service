package iuh.fit.socialservice.application.port.external;

import iuh.fit.socialservice.domain.enums.PostSport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chấm điểm bài viết ứng viên cho một người xem (ai-service). Social-service tự chọn ứng viên trong số bài người xem
 * được thấy, nên AI không bao giờ làm lộ bài riêng tư.
 */
public interface PostRanker {
    record Candidate(UUID postId, UUID authorId, PostSport sport, long likeCount, long commentCount,
                     LocalDateTime publishedAt) { }

    /** Điểm theo mã bài; rỗng khi AI không dùng được, người gọi giữ thứ tự mới nhất. */
    Map<UUID, Double> rank(UUID viewerId, List<Candidate> candidates);
}
