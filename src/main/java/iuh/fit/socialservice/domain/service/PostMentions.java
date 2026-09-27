package iuh.fit.socialservice.domain.service;

import iuh.fit.socialservice.domain.entity.Mention;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lam sach danh sach nhac ten do client gui len (bai viet va binh luan dung chung). */
public final class PostMentions {
    public static final int MAX_MENTIONS = 20;
    public static final int MAX_NAME_LENGTH = 100;

    private PostMentions() {
    }

    /**
     * Giu nguoi con xuat hien dang "@ten" trong noi dung; bo trung, bo chinh nguoi viet va nguoi co
     * quan he chan voi ho; toi da {@link #MAX_MENTIONS}. Client khong the nhac ai ma khong viet ten ra.
     */
    public static List<Mention> sanitize(List<Mention> requested, String content, UUID authorId, Set<UUID> blockedIds) {
        if (requested == null || content == null || content.isBlank()) return List.of();
        Set<UUID> seen = new HashSet<>();
        List<Mention> result = new ArrayList<>();
        for (Mention mention : requested) {
            if (mention == null || mention.userId() == null || mention.name() == null) continue;
            String name = mention.name().strip();
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || !content.contains("@" + name)) continue;
            UUID userId = mention.userId();
            if (userId.equals(authorId) || blockedIds.contains(userId) || !seen.add(userId)) continue;
            result.add(new Mention(userId, name));
            if (result.size() == MAX_MENTIONS) break;
        }
        return result;
    }

    /** Nguoi co trong {@code current} nhung khong co trong {@code previous}: chi ho moi can duoc bao. */
    public static List<UUID> added(List<Mention> previous, List<Mention> current) {
        Set<UUID> before = new HashSet<>();
        if (previous != null) previous.forEach(mention -> before.add(mention.userId()));
        return current.stream().map(Mention::userId).filter(id -> !before.contains(id)).toList();
    }
}
