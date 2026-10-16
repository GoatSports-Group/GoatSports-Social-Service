package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository.Interaction;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository.InteractionKind;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository.PostFact;
import iuh.fit.socialservice.application.service.SocialStatsService.ActivityStats;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.service.PostTags;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialStatsServiceImplTest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 12);   // thứ Hai
    private static final LocalDate TO = LocalDate.of(2026, 10, 18);

    /** Kỳ trước của [12/10, 18/10] là [5/10, 11/10]; bài "old" đăng trước cả hai kỳ. */
    private static final PostFact OLD = post("old", "a", LocalDateTime.of(2026, 9, 1, 8, 0), PostSport.TENNIS, null,
            List.of("tennis"), null);
    private static final List<PostFact> POSTS = List.of(
            post("p1", "a", LocalDateTime.of(2026, 10, 12, 19, 5), PostSport.FOOTBALL, null, List.of("bóng_đá", "timdoi"), false),
            post("p2", "b", LocalDateTime.of(2026, 10, 13, 7, 0), PostSport.BADMINTON, null, List.of("bóng_đá"), null),
            post("s1", "c", LocalDateTime.of(2026, 10, 13, 20, 0), PostSport.FOOTBALL, "p1", List.of(), null),
            post("prev", "d", LocalDateTime.of(2026, 10, 8, 9, 0), PostSport.FOOTBALL, null, List.of("bong_da"), null));
    private static final List<Interaction> INTERACTIONS = List.of(
            new Interaction(InteractionKind.LIKE, "b", "p1", LocalDateTime.of(2026, 10, 12, 19, 30)),
            new Interaction(InteractionKind.LIKE, "c", "p1", LocalDateTime.of(2026, 10, 12, 19, 40)),
            new Interaction(InteractionKind.COMMENT, "e", "p1", LocalDateTime.of(2026, 10, 12, 21, 0)),
            new Interaction(InteractionKind.SAVE, "e", "p2", LocalDateTime.of(2026, 10, 13, 8, 0)),
            new Interaction(InteractionKind.LIKE, "f", "old", LocalDateTime.of(2026, 10, 14, 6, 0)),
            new Interaction(InteractionKind.LIKE, "g", "prev", LocalDateTime.of(2026, 10, 9, 10, 0)));

    private final SocialStatsServiceImpl service = new SocialStatsServiceImpl(new SocialStatsRepository() {
        @Override
        public List<PostFact> postsPublished(LocalDateTime from, LocalDateTime to) {
            assertThat(from).isEqualTo(LocalDateTime.of(2026, 10, 5, 0, 0));
            assertThat(to).isEqualTo(LocalDateTime.of(2026, 10, 19, 0, 0));
            return POSTS;
        }

        @Override
        public List<PostFact> postsById(Collection<String> postIds) {
            assertThat(postIds).containsExactly("old");
            return List.of(OLD);
        }

        @Override
        public List<Interaction> interactions(LocalDateTime from, LocalDateTime to) {
            return INTERACTIONS;
        }
    });

    @Test
    void splitsCurrentAndPreviousPeriods() throws ServiceException {
        ActivityStats stats = service.getActivityStats(FROM, TO);
        assertThat(stats.totals().posts()).isEqualTo(2);
        assertThat(stats.totals().shares()).isEqualTo(1);
        assertThat(stats.totals().likes()).isEqualTo(3);
        assertThat(stats.totals().comments()).isEqualTo(1);
        assertThat(stats.totals().saves()).isEqualTo(1);
        // a, b, c đăng; e, f tương tác.
        assertThat(stats.totals().activeUsers()).isEqualTo(5);
        assertThat(stats.totals().postingUsers()).isEqualTo(2);
        assertThat(stats.totals().playerCalls()).isEqualTo(1);
        assertThat(stats.totals().playerCallsFilled()).isZero();
        assertThat(stats.previous().posts()).isEqualTo(1);
        assertThat(stats.previous().activeUsers()).isEqualTo(2);
    }

    @Test
    void buildsDailySeriesHeatmapAndRankings() throws ServiceException {
        ActivityStats stats = service.getActivityStats(FROM, TO);
        assertThat(stats.byDay()).hasSize(7);
        assertThat(stats.byDay().get(0)).satisfies(day -> {
            assertThat(day.posts()).isEqualTo(1);
            assertThat(day.likes()).isEqualTo(2);
            assertThat(day.comments()).isEqualTo(1);
            assertThat(day.activeUsers()).isEqualTo(4);
        });
        assertThat(stats.byDay().get(1).shares()).isEqualTo(1);
        assertThat(stats.heatmap()).hasSize(7 * 24);
        assertThat(stats.heatmap().stream().filter(cell -> cell.dayOfWeek() == 1 && cell.hour() == 19).findFirst()
                .orElseThrow().count()).isEqualTo(3);

        // Bóng đá: 2 thích + 1 bình luận + 1 chia sẻ; tennis có lượt thích trên bài cũ; lượt lưu không tính.
        assertThat(stats.bySport()).extracting(row -> row.sport() + ":" + row.posts() + ":" + row.interactions())
                .containsExactly("FOOTBALL:1:4", "TENNIS:0:1", "BADMINTON:1:0");
        // Kỳ trước viết không dấu vẫn cùng khóa nên được so với kỳ này.
        assertThat(stats.trendingTags().get(0)).satisfies(tag -> {
            assertThat(tag.tag()).isEqualTo("bóng_đá");
            assertThat(tag.posts()).isEqualTo(2);
            assertThat(tag.authors()).isEqualTo(2);
            assertThat(tag.previousPosts()).isEqualTo(1);
        });
        assertThat(stats.topPosts()).extracting(post -> post.postId()).containsExactly("p1", "old");
        assertThat(stats.topPosts().get(0).shares()).isEqualTo(1);
        // b: 1 bài (3) + 1 lượt thích (1) vượt a: 1 bài (3).
        assertThat(stats.topMembers().get(0).userId()).isEqualTo("b");
    }

    @Test
    void rejectsInvalidRanges() {
        assertThatThrownBy(() -> service.getActivityStats(TO, FROM)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.getActivityStats(FROM.minusDays(400), TO)).isInstanceOf(ServiceException.class);
    }

    private static PostFact post(String id, String author, LocalDateTime at, PostSport sport, String sharedPostId,
                                 List<String> tags, Boolean filled) {
        List<String> keys = tags.stream().map(PostTags::key).toList();
        return new PostFact(id, author, at, sport, sharedPostId, tags, keys, filled, "Nội dung " + id);
    }
}
