package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.HighlightKind;

import java.util.List;
import java.util.UUID;

public record PostHighlight(
        HighlightKind kind,
        UUID refId,
        String title,
        String headline,
        String subtitle,
        String result,
        List<Side> sides,
        List<Stat> stats,
        UUID clubId,
        String clubName,
        List<UUID> tagged
) {
    public static final int MAX_TAGGED = 50;

    /** Mot ben trong tran: ten, ty so (null khi chua dau), co thang khong. */
    public record Side(String name, Integer score, boolean winner) {
    }

    public record Stat(String label, String value) {
    }

    public PostHighlight withTagged(List<UUID> value) {
        return new PostHighlight(kind, refId, title, headline, subtitle, result, sides, stats, clubId, clubName, value);
    }
}
