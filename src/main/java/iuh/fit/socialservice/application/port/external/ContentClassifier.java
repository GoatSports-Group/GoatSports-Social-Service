package iuh.fit.socialservice.application.port.external;

import iuh.fit.socialservice.domain.entity.AiVerdict;

import java.util.Optional;

/** Phân loại một đoạn chữ bị báo cáo bằng AI (ai-service). Chỉ gửi phần chữ, không gửi mã hay tên người dùng. */
public interface ContentClassifier {
    /**
     * Rỗng khi AI tạm thời không dùng được (chưa cấu hình, hết hạn mức, mạng lỗi): để nguyên, lần quét sau thử lại.
     *
     * @throws Unclassifiable khi AI trả lời nhưng không dùng được cho riêng đoạn chữ này
     */
    Optional<AiVerdict> classify(String kind, String text, String reportReason);

    class Unclassifiable extends RuntimeException {
        public Unclassifiable(String message) {
            super(message);
        }
    }
}
