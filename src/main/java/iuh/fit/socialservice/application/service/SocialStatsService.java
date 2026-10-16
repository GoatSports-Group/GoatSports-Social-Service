package iuh.fit.socialservice.application.service;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.domain.enums.PostSport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Hoạt động cộng đồng cho dashboard admin: xu hướng bài viết, người dùng tích cực, mức tương tác, môn và chủ đề nổi. */
public interface SocialStatsService {
    int MAX_DAYS = 366;

    /** Kỳ [fromDate, toDate] (theo ngày, tính cả hai đầu) kèm tổng của kỳ ngay trước cùng độ dài để so sánh. */
    ActivityStats getActivityStats(LocalDate fromDate, LocalDate toDate) throws ServiceException;

    /**
     * {@code posts} không tính bài chia sẻ (đếm riêng ở {@code shares}); {@code activeUsers}: người có ít nhất một
     * lần đăng, chia sẻ, bình luận, thích hoặc lưu.
     */
    record Totals(long posts, long shares, long comments, long likes, long saves, long activeUsers, long postingUsers,
                  long playerCalls, long playerCallsFilled) { }

    record Day(LocalDate date, long posts, long comments, long likes, long shares, long activeUsers) { }

    /** {@code interactions}: thích + bình luận + chia sẻ trong kỳ trên bài của môn đó (kể cả bài đăng trước kỳ). */
    record SportRow(PostSport sport, long posts, long interactions) { }

    record TagRow(String tag, long posts, long authors, long previousPosts) { }

    /** {@code dayOfWeek}: 1 = thứ Hai … 7 = Chủ nhật; {@code count}: bài + bình luận + lượt thích bắt đầu trong giờ đó. */
    record HeatCell(int dayOfWeek, int hour, long count) { }

    record TopPost(String postId, String authorId, String excerpt, PostSport sport, LocalDateTime publishedAt,
                   long likes, long comments, long shares) { }

    record TopMember(String userId, long posts, long comments, long reactions) { }

    record ActivityStats(LocalDate fromDate, LocalDate toDate, Totals totals, Totals previous, List<Day> byDay,
                         List<SportRow> bySport, List<TagRow> trendingTags, List<HeatCell> heatmap,
                         List<TopPost> topPosts, List<TopMember> topMembers) { }
}
