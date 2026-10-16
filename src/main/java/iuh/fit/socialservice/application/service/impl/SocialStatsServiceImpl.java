package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository.Interaction;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository.InteractionKind;
import iuh.fit.socialservice.application.port.persistence.SocialStatsRepository.PostFact;
import iuh.fit.socialservice.application.service.SocialStatsService;
import iuh.fit.socialservice.domain.enums.PostSport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lấy dữ kiện thô của kỳ đang xem và kỳ trước một lần rồi tổng hợp ở Java.
 * ponytail: quét mọi bài / tương tác trong 2 kỳ (≤ 2 × 366 ngày); chuyển sang bảng đếm theo ngày cập nhật dần
 * khi tương tác lên hàng triệu mỗi kỳ.
 */
@Service
@RequiredArgsConstructor
public class SocialStatsServiceImpl implements SocialStatsService {
    private static final int TOP_TAGS = 8;
    private static final int TOP_POSTS = 5;
    private static final int TOP_MEMBERS = 5;
    private static final int EXCERPT = 140;

    private final SocialStatsRepository repository;

    @Override
    public ActivityStats getActivityStats(LocalDate fromDate, LocalDate toDate) throws ServiceException {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ServiceException("Khoảng ngày không hợp lệ");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > MAX_DAYS) throw new ServiceException("Chỉ thống kê tối đa " + MAX_DAYS + " ngày mỗi lần");
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay();
        LocalDateTime previousFrom = from.minusDays(days);

        List<PostFact> allPosts = repository.postsPublished(previousFrom, to);
        List<Interaction> allInteractions = repository.interactions(previousFrom, to);
        Predicate<LocalDateTime> current = at -> !at.isBefore(from);
        List<PostFact> posts = allPosts.stream().filter(post -> current.test(post.publishedAt())).toList();
        List<PostFact> previousPosts = allPosts.stream().filter(post -> !current.test(post.publishedAt())).toList();
        List<Interaction> interactions = allInteractions.stream().filter(item -> current.test(item.at())).toList();
        List<Interaction> previousInteractions = allInteractions.stream().filter(item -> !current.test(item.at())).toList();

        Map<String, PostFact> postsById = postsById(allPosts, interactions, posts);
        return new ActivityStats(fromDate, toDate, totals(posts, interactions), totals(previousPosts, previousInteractions),
                byDay(fromDate, toDate, posts, interactions), bySport(posts, interactions, postsById),
                trendingTags(originals(posts), originals(previousPosts)), heatmap(posts, interactions),
                topPosts(posts, interactions, postsById), topMembers(posts, interactions));
    }

    /** Bài của kỳ đã có sẵn; bài cũ hơn được tương tác trong kỳ thì hỏi thêm theo mã. */
    private Map<String, PostFact> postsById(List<PostFact> known, List<Interaction> interactions, List<PostFact> posts) {
        Map<String, PostFact> byId = new HashMap<>();
        known.forEach(post -> byId.put(post.postId(), post));
        Set<String> missing = Stream.concat(interactions.stream().map(Interaction::postId),
                        posts.stream().map(PostFact::sharedPostId))
                .filter(id -> id != null && !byId.containsKey(id)).collect(Collectors.toSet());
        if (!missing.isEmpty()) repository.postsById(missing).forEach(post -> byId.put(post.postId(), post));
        return byId;
    }

    private static List<PostFact> originals(List<PostFact> posts) {
        return posts.stream().filter(post -> post.sharedPostId() == null).toList();
    }

    private static long count(List<Interaction> interactions, InteractionKind kind) {
        return interactions.stream().filter(item -> item.kind() == kind).count();
    }

    static Totals totals(List<PostFact> posts, List<Interaction> interactions) {
        List<PostFact> originals = originals(posts);
        Set<String> active = new HashSet<>();
        posts.forEach(post -> active.add(post.authorId()));
        interactions.forEach(item -> active.add(item.userId()));
        return new Totals(originals.size(), posts.size() - originals.size(), count(interactions, InteractionKind.COMMENT),
                count(interactions, InteractionKind.LIKE), count(interactions, InteractionKind.SAVE), active.size(),
                originals.stream().map(PostFact::authorId).distinct().count(),
                originals.stream().filter(post -> post.playerCallFilled() != null).count(),
                originals.stream().filter(post -> Boolean.TRUE.equals(post.playerCallFilled())).count());
    }

    private static List<Day> byDay(LocalDate fromDate, LocalDate toDate, List<PostFact> posts, List<Interaction> interactions) {
        Map<LocalDate, long[]> counts = new HashMap<>();
        Map<LocalDate, Set<String>> active = new HashMap<>();
        for (PostFact post : posts) {
            LocalDate date = post.publishedAt().toLocalDate();
            counts.computeIfAbsent(date, key -> new long[4])[post.sharedPostId() == null ? 0 : 3]++;
            active.computeIfAbsent(date, key -> new HashSet<>()).add(post.authorId());
        }
        for (Interaction item : interactions) {
            LocalDate date = item.at().toLocalDate();
            if (item.kind() != InteractionKind.SAVE) {
                counts.computeIfAbsent(date, key -> new long[4])[item.kind() == InteractionKind.COMMENT ? 1 : 2]++;
            }
            active.computeIfAbsent(date, key -> new HashSet<>()).add(item.userId());
        }
        List<Day> days = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            long[] day = counts.getOrDefault(date, new long[4]);
            days.add(new Day(date, day[0], day[1], day[2], day[3], active.getOrDefault(date, Set.of()).size()));
        }
        return days;
    }

    private static List<SportRow> bySport(List<PostFact> posts, List<Interaction> interactions, Map<String, PostFact> byId) {
        Map<PostSport, long[]> rows = new LinkedHashMap<>();
        for (PostFact post : posts) {
            PostSport sport = post.sharedPostId() == null ? post.sport() : sportOf(byId.get(post.sharedPostId()));
            if (sport == null) continue;
            rows.computeIfAbsent(sport, key -> new long[2])[post.sharedPostId() == null ? 0 : 1]++;
        }
        for (Interaction item : interactions) {
            PostSport sport = item.kind() == InteractionKind.SAVE ? null : sportOf(byId.get(item.postId()));
            if (sport != null) rows.computeIfAbsent(sport, key -> new long[2])[1]++;
        }
        return rows.entrySet().stream()
                .map(entry -> new SportRow(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted(Comparator.comparingLong(SportRow::interactions).reversed()
                        .thenComparing(Comparator.comparingLong(SportRow::posts).reversed()))
                .toList();
    }

    private static PostSport sportOf(PostFact post) {
        return post == null ? null : post.sport();
    }

    /** Chủ đề theo khóa (bỏ dấu); hiển thị cách viết phổ biến nhất. Xếp theo số bài trong kỳ. */
    static List<TagRow> trendingTags(List<PostFact> posts, List<PostFact> previousPosts) {
        Map<String, Map<String, Long>> forms = new HashMap<>();
        Map<String, Set<String>> authors = new HashMap<>();
        Map<String, Long> counts = new HashMap<>();
        for (PostFact post : posts) {
            for (int index = 0; index < post.tagKeys().size() && index < post.tags().size(); index++) {
                String key = post.tagKeys().get(index);
                counts.merge(key, 1L, Long::sum);
                forms.computeIfAbsent(key, item -> new HashMap<>()).merge(post.tags().get(index), 1L, Long::sum);
                authors.computeIfAbsent(key, item -> new HashSet<>()).add(post.authorId());
            }
        }
        Map<String, Long> previous = previousPosts.stream().flatMap(post -> post.tagKeys().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(entry -> -authors.get(entry.getKey()).size())
                        .thenComparing(Map.Entry::getKey))
                .limit(TOP_TAGS)
                .map(entry -> new TagRow(forms.get(entry.getKey()).entrySet().stream()
                        .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry::getKey,
                                Comparator.reverseOrder()))
                        .map(Map.Entry::getKey).orElse(entry.getKey()),
                        entry.getValue(), authors.get(entry.getKey()).size(), previous.getOrDefault(entry.getKey(), 0L)))
                .toList();
    }

    /** Đủ 7 × 24 ô (ô trống = 0) để client vẽ thẳng. */
    private static List<HeatCell> heatmap(List<PostFact> posts, List<Interaction> interactions) {
        long[][] cells = new long[7][24];
        posts.stream().filter(post -> post.sharedPostId() == null).map(PostFact::publishedAt)
                .forEach(at -> cells[at.getDayOfWeek().getValue() - 1][at.getHour()]++);
        interactions.stream().filter(item -> item.kind() != InteractionKind.SAVE).map(Interaction::at)
                .forEach(at -> cells[at.getDayOfWeek().getValue() - 1][at.getHour()]++);
        List<HeatCell> heatmap = new ArrayList<>(7 * 24);
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) heatmap.add(new HeatCell(day + 1, hour, cells[day][hour]));
        }
        return heatmap;
    }

    /** Điểm nổi bật trong kỳ: thích + 2 × bình luận + 3 × chia sẻ (chia sẻ kéo người mới tới nhiều nhất). */
    private static List<TopPost> topPosts(List<PostFact> posts, List<Interaction> interactions, Map<String, PostFact> byId) {
        Map<String, long[]> tally = new HashMap<>();
        for (Interaction item : interactions) {
            if (item.kind() == InteractionKind.SAVE) continue;
            tally.computeIfAbsent(item.postId(), key -> new long[3])[item.kind() == InteractionKind.LIKE ? 0 : 1]++;
        }
        posts.stream().map(PostFact::sharedPostId).filter(id -> id != null)
                .forEach(id -> tally.computeIfAbsent(id, key -> new long[3])[2]++);
        return tally.entrySet().stream()
                .filter(entry -> byId.containsKey(entry.getKey()) && byId.get(entry.getKey()).sharedPostId() == null)
                .sorted(Comparator.<Map.Entry<String, long[]>>comparingLong(entry -> score(entry.getValue())).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(TOP_POSTS)
                .map(entry -> {
                    PostFact post = byId.get(entry.getKey());
                    long[] counts = entry.getValue();
                    return new TopPost(post.postId(), post.authorId(), excerpt(post.content()), post.sport(),
                            post.publishedAt(), counts[0], counts[1], counts[2]);
                })
                .toList();
    }

    private static long score(long[] counts) {
        return counts[0] + 2 * counts[1] + 3 * counts[2];
    }

    private static String excerpt(String content) {
        if (content == null) return "";
        String text = content.strip().replaceAll("\\s+", " ");
        return text.length() <= EXCERPT ? text : text.substring(0, EXCERPT - 1).stripTrailing() + "…";
    }

    /** Người đóng góp nhiều nhất: đăng bài nặng nhất (3), rồi bình luận (2), rồi thích (1). */
    private static List<TopMember> topMembers(List<PostFact> posts, List<Interaction> interactions) {
        Map<String, long[]> tally = new HashMap<>();
        originals(posts).forEach(post -> tally.computeIfAbsent(post.authorId(), key -> new long[3])[0]++);
        for (Interaction item : interactions) {
            if (item.kind() == InteractionKind.SAVE) continue;
            tally.computeIfAbsent(item.userId(), key -> new long[3])[item.kind() == InteractionKind.COMMENT ? 1 : 2]++;
        }
        return tally.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, long[]>>comparingLong(entry ->
                                3 * entry.getValue()[0] + 2 * entry.getValue()[1] + entry.getValue()[2]).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(TOP_MEMBERS)
                .map(entry -> new TopMember(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();
    }
}
