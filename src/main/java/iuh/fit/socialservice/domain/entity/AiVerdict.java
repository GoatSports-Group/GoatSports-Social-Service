package iuh.fit.socialservice.domain.entity;

import java.time.LocalDateTime;

/**
 * Gợi ý kiểm duyệt của AI cho một báo cáo: nhãn (NONE, SPAM, HARASSMENT, HATE, SEXUAL, VIOLENCE, SCAM, OTHER),
 * mức 0–3 và một câu lý do. Chỉ để admin xếp hàng đợi; AI không bao giờ tự ẩn hay gỡ nội dung.
 */
public record AiVerdict(String label, Integer severity, String reason, LocalDateTime checkedAt) {
}
