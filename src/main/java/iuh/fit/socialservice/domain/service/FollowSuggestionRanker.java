package iuh.fit.socialservice.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Xep hang "Goi y theo doi" theo moi lien he that voi nguoi xem, hoat dong dang bai chi de pha hoa.
 *
 * <p>Diem moi tin hieu (cong don, moi loai ly do chi tinh mot lan cho moi nguoi):
 * <ul>
 *   <li>Da ghep tran voi ban (chung nhom chat MATCH): 50</li>
 *   <li>Cung CLB (chung nhom chat CLB): 40</li>
 *   <li>Da nhan tin voi ban (chat 1-1): 30</li>
 *   <li>Ban chung: 15 moi nguoi, toi da 3 nguoi (45)</li>
 *   <li>Cung du giai (chung nhom chat giai): 20</li>
 *   <li>Hay dang bai ve mon ban choi (30 ngay): 10 + so bai (toi da 10)</li>
 *   <li>Dang bai cong khai (30 ngay): so bai (toi da 10)</li>
 * </ul>
 * Dong ly do hien hai ly do nang ky nhat, vd "Da ghep tran voi ban · 2 ban chung".
 */
public final class FollowSuggestionRanker {
    private final Set<UUID> excluded;
    private final Map<UUID, Candidate> candidates = new LinkedHashMap<>();

    public FollowSuggestionRanker(Set<UUID> excluded) {
        this.excluded = excluded;
    }

    public record Suggestion(UUID userId, int score, long postCount, String reason) {
    }

    public void playedWith(UUID userId) {
        add(userId, 50, "Đã ghép trận với bạn");
    }

    public void sameClub(UUID userId, String clubName) {
        String name = clubName == null ? "" : clubName.strip();
        // Ten CLB thuong da bat dau bang "CLB": khong viet thanh "Cung CLB CLB ...".
        add(userId, 40, name.isEmpty() ? "Cùng câu lạc bộ"
                : name.regionMatches(true, 0, "CLB ", 0, 4) ? "Cùng " + name : "Cùng CLB " + name);
    }

    public void chattedWith(UUID userId) {
        add(userId, 30, "Đã nhắn tin với bạn");
    }

    public void mutualFriends(UUID userId, int count) {
        if (count > 0) add(userId, 15 * Math.min(count, 3), count + " bạn chung");
    }

    public void sameTournament(UUID userId, String tournamentName) {
        add(userId, 20, tournamentName == null || tournamentName.isBlank() ? "Cùng dự giải đấu" : "Cùng dự " + tournamentName);
    }

    public void postsInMySports(UUID userId, long postCount) {
        if (add(userId, 10 + (int) Math.min(postCount, 10), "Hay đăng bài về môn bạn chơi")) seen(userId, postCount);
    }

    public void activePoster(UUID userId, long postCount) {
        if (add(userId, (int) Math.min(postCount, 10), postCount + " bài trong 30 ngày")) seen(userId, postCount);
    }

    public List<Suggestion> top(int limit) {
        return candidates.values().stream()
                .sorted(Comparator.comparingInt(Candidate::score).reversed()
                        .thenComparing(Comparator.comparingLong(Candidate::postCount).reversed()))
                .limit(limit)
                .map(candidate -> new Suggestion(candidate.userId, candidate.score, candidate.postCount,
                        candidate.reason()))
                .toList();
    }

    /** @return false khi nguoi nay bi loai hoac ly do nay da duoc tinh. */
    private boolean add(UUID userId, int weight, String reason) {
        if (userId == null || excluded.contains(userId)) return false;
        Candidate candidate = candidates.computeIfAbsent(userId, Candidate::new);
        // Cung loai ly do (vd. hai CLB chung) chi cong diem mot lan, ly do dau tien duoc giu.
        if (candidate.reasons.stream().anyMatch(item -> item.kind.equals(kindOf(reason)))) return true;
        candidate.reasons.add(new Reason(kindOf(reason), weight, reason));
        candidate.score += weight;
        return true;
    }

    private void seen(UUID userId, long postCount) {
        Candidate candidate = candidates.get(userId);
        candidate.postCount = Math.max(candidate.postCount, postCount);
    }

    /** "Cung CLB A" va "Cung CLB B" la cung mot loai ly do. */
    private static String kindOf(String reason) {
        if (reason.startsWith("Cùng CLB") || reason.startsWith("Cùng clb") || reason.startsWith("Cùng câu lạc bộ")) return "club";
        if (reason.startsWith("Cùng dự")) return "tournament";
        if (reason.endsWith("bạn chung")) return "mutual";
        if (reason.endsWith("bài trong 30 ngày")) return "active";
        return reason;
    }

    private record Reason(String kind, int weight, String text) {
    }

    private static final class Candidate {
        private final UUID userId;
        private final List<Reason> reasons = new ArrayList<>();
        private int score;
        private long postCount;

        private Candidate(UUID userId) {
            this.userId = userId;
        }

        int score() {
            return score;
        }

        long postCount() {
            return postCount;
        }

        String reason() {
            return reasons.stream().sorted(Comparator.comparingInt(Reason::weight).reversed()).limit(2)
                    .map(Reason::text).reduce((first, second) -> first + " · " + second).orElse("");
        }
    }
}
