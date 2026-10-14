package iuh.fit.socialservice.infrastructure.external.adapter;

import feign.FeignException;
import iuh.fit.socialservice.application.port.external.ContentClassifier;
import iuh.fit.socialservice.domain.entity.AiVerdict;
import iuh.fit.socialservice.infrastructure.external.client.AiModerationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentClassifierAdapter implements ContentClassifier {
    // Khớp giới hạn của ai-service; phần đầu đủ để nhận ra vi phạm.
    private static final int MAX_TEXT = 4000;

    private final AiModerationClient client;

    @Override
    public Optional<AiVerdict> classify(String kind, String text, String reportReason) {
        try {
            AiModerationClient.Verdict verdict = client.classify(new AiModerationClient.ClassifyRequest(
                    kind, text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text, reportReason));
            return Optional.of(new AiVerdict(
                    verdict.label(), verdict.severity(), verdict.reason(), LocalDateTime.now()));
        } catch (FeignException.UnprocessableEntity | FeignException.BadRequest exception) {
            throw new Unclassifiable(exception.getMessage());
        } catch (FeignException exception) {
            log.warn("AI moderation unavailable (status {}), retrying on the next scan", exception.status());
            return Optional.empty();
        }
    }
}
