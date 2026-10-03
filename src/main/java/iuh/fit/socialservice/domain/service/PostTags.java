package iuh.fit.socialservice.domain.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trich hashtag tu noi dung bai viet. Client hien thi theo cung quy tac (community-view.ts).
 *
 * <p>Hai dang cua mot tag: {@link #extract} giu cach viet (co dau, chu thuong, Unicode NFC) de hien thi;
 * {@link #key} la khoa so khop (bo dau, d thay cho đ) de "#BóngĐá", "#bóngđá" va "#bongda" la cung mot chu de
 * khi loc, dem va goi y.
 */
public final class PostTags {
    public static final int MAX_TAGS = 10;

    // "#" khong dung lien sau mot chu/so (tranh "abc#def", ma mau "&#123;"), 2-40 ky tu chu/so/_.
    // \p{M}: dau tieng Viet o dang to hop (NFD) van thuoc tu.
    private static final Pattern TAG = Pattern.compile("(?<![\\p{L}\\p{M}\\p{N}_&])#([\\p{L}\\p{M}\\p{N}_]{2,40})");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private PostTags() {
    }

    /**
     * Chu thuong, dang NFC (ban phim dien thoai hay go dau to hop), bo trung theo {@link #key}, giu thu tu xuat hien,
     * toi da {@link #MAX_TAGS}.
     */
    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        Map<String, String> byKey = new LinkedHashMap<>();
        Matcher matcher = TAG.matcher(Normalizer.normalize(content, Normalizer.Form.NFC));
        while (matcher.find() && byKey.size() < MAX_TAGS) {
            String tag = matcher.group(1).toLowerCase(Locale.ROOT);
            byKey.putIfAbsent(key(tag), tag);
        }
        return new ArrayList<>(byKey.values());
    }

    /** Khoa so khop cua mot tag: bo dau, đ → d, chu thuong. */
    public static String key(String tag) {
        String plain = MARKS.matcher(Normalizer.normalize(tag, Normalizer.Form.NFD)).replaceAll("");
        return plain.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    /** Chuan hoa tu khoa loc (bo "#") thanh {@link #key}; null neu khong hop le. */
    public static String normalize(String tag) {
        if (tag == null) return null;
        String value = Normalizer.normalize(tag.strip(), Normalizer.Form.NFC);
        if (value.startsWith("#")) value = value.substring(1);
        return value.matches("[\\p{L}\\p{M}\\p{N}_]{2,40}") ? key(value) : null;
    }
}
