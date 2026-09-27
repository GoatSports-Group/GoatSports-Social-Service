package iuh.fit.socialservice.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Trich hashtag tu noi dung bai viet. Client hien thi theo cung quy tac (community-view.ts). */
public final class PostTags {
    public static final int MAX_TAGS = 10;

    // "#" khong dung lien sau mot chu/so (tranh "abc#def", ma mau "&#123;"), 2-40 ky tu chu/so/_.
    // \p{M}: dau tieng Viet o dang to hop (NFD) van thuoc tu.
    private static final Pattern TAG = Pattern.compile("(?<![\\p{L}\\p{M}\\p{N}_&])#([\\p{L}\\p{M}\\p{N}_]{2,40})");

    private PostTags() {
    }

    /** Chu thuong, bo trung, giu thu tu xuat hien, toi da {@link #MAX_TAGS}. */
    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        Set<String> tags = new LinkedHashSet<>();
        Matcher matcher = TAG.matcher(content);
        while (matcher.find() && tags.size() < MAX_TAGS) {
            tags.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(tags);
    }

    /** Chuan hoa tu khoa loc (bo "#", chu thuong); null neu khong hop le. */
    public static String normalize(String tag) {
        if (tag == null) return null;
        String value = tag.strip();
        if (value.startsWith("#")) value = value.substring(1);
        return value.matches("[\\p{L}\\p{M}\\p{N}_]{2,40}") ? value.toLowerCase(Locale.ROOT) : null;
    }
}
