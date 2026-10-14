package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.ModerationTargetResponseCommand;
import iuh.fit.socialservice.application.port.external.ContentClassifier;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.service.SocialModerationService;
import iuh.fit.socialservice.domain.entity.AiVerdict;
import iuh.fit.socialservice.domain.entity.ContentReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Cho AI đọc trước các báo cáo đang mở để hàng đợi admin xếp mục nặng lên đầu. Chạy nền, vài báo cáo mỗi lượt,
 * để không vượt hạn mức gọi LLM. AI không dùng được thì dừng lượt và thử lại sau, không tính là lỗi của báo cáo;
 * AI trả lời sai định dạng thì tính một lần thử, quá {@link #MAX_ATTEMPTS} lần thì bỏ qua báo cáo đó.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportAiTriageService {
    static final int MAX_ATTEMPTS = 3;

    private final ContentReportRepository reportRepository;
    private final SocialModerationService moderationService;
    private final ContentClassifier classifier;

    @Value("${moderation.ai.batch-size:2}")
    private int batchSize = 2;

    static final Duration FIRST_BACKOFF = Duration.ofMinutes(1);
    static final Duration MAX_BACKOFF = Duration.ofMinutes(30);
    private Duration backoff = Duration.ZERO;
    private Instant pausedUntil = Instant.EPOCH;

    @Scheduled(initialDelayString = "${moderation.ai.initial-delay-ms:60000}",
            fixedDelayString = "${moderation.ai.scan-ms:30000}")
    public void triagePending() {
        if (isPaused()) return;
        for (ContentReport report : reportRepository.findAwaitingAiVerdict(MAX_ATTEMPTS, batchSize)) {
            ModerationTargetResponseCommand target =
                    moderationService.getTarget(report.getTargetType(), report.getTargetId());
            if (!target.found() || target.content() == null || target.content().isBlank()) {
                // Chỉ có ảnh hoặc nội dung đã mất: AI không có chữ để đọc, admin xem như cũ.
                reportRepository.addAiAttempts(report.getReportId(), MAX_ATTEMPTS);
                continue;
            }
            try {
                Optional<AiVerdict> verdict =
                        classifier.classify(report.getTargetType().name(), target.content(), report.getReason());
                if (verdict.isEmpty()) {
                    backOff();
                    return;
                }
                backoff = Duration.ZERO;
                reportRepository.saveAiVerdict(report.getReportId(), verdict.get());
            } catch (ContentClassifier.Unclassifiable exception) {
                log.warn("AI could not classify report {}: {}", report.getReportId(), exception.getMessage());
                reportRepository.addAiAttempts(report.getReportId(), 1);
            }
        }
    }

    private void backOff() {
        backoff = backoff.isZero() ? FIRST_BACKOFF : backoff.multipliedBy(2);
        if (backoff.compareTo(MAX_BACKOFF) > 0) backoff = MAX_BACKOFF;
        pausedUntil = Instant.now().plus(backoff);
        log.info("AI moderation unavailable, pausing for {} min", backoff.toMinutes());
    }

    boolean isPaused() {
        return Instant.now().isBefore(pausedUntil);
    }
}
