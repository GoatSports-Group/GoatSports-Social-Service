package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.response.ModerationTargetResponseCommand;
import iuh.fit.socialservice.application.port.external.ContentClassifier;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.service.SocialModerationService;
import iuh.fit.socialservice.domain.entity.AiVerdict;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAiTriageServiceTest {
    @Mock
    private ContentReportRepository reportRepository;
    @Mock
    private SocialModerationService moderationService;
    @Mock
    private ContentClassifier classifier;

    private ReportAiTriageService service;

    private final ContentReport first = report("spam spam");
    private final ContentReport second = report("xúc phạm");

    @BeforeEach
    void setUp() {
        service = new ReportAiTriageService(reportRepository, moderationService, classifier);
        when(reportRepository.findAwaitingAiVerdict(eq(ReportAiTriageService.MAX_ATTEMPTS), anyInt()))
                .thenReturn(List.of(first, second));
    }

    @Test
    void savesVerdictForEachReportWithText() {
        target(first, "Mua giày giá rẻ tại link này");
        target(second, "Đồ ngu");
        AiVerdict spam = new AiVerdict("SPAM", 1, "Quảng cáo.", LocalDateTime.now());
        AiVerdict insult = new AiVerdict("HARASSMENT", 2, "Chửi người khác.", LocalDateTime.now());
        when(classifier.classify("POST", "Mua giày giá rẻ tại link này", "spam spam")).thenReturn(Optional.of(spam));
        when(classifier.classify("POST", "Đồ ngu", "xúc phạm")).thenReturn(Optional.of(insult));

        service.triagePending();

        verify(reportRepository).saveAiVerdict(first.getReportId(), spam);
        verify(reportRepository).saveAiVerdict(second.getReportId(), insult);
    }

    @Test
    void stopsTheScanWithoutCountingAttemptsWhenAiIsUnavailable() {
        target(first, "Nội dung");
        when(classifier.classify(any(), any(), any())).thenReturn(Optional.empty());

        service.triagePending();

        verify(classifier).classify(any(), any(), any());
        verify(reportRepository, never()).saveAiVerdict(any(), any());
        verify(reportRepository, never()).addAiAttempts(any(), anyInt());

        // Gemini 429: lượt quét kế tiếp không gọi lại ngay mà chờ hết thời gian lùi.
        service.triagePending();
        verify(classifier, times(1)).classify(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(service.isPaused()).isTrue();
    }

    @Test
    void countsOneAttemptWhenAiCannotReadTheTextAndMovesOn() {
        target(first, "???");
        target(second, "Đồ ngu");
        when(classifier.classify("POST", "???", "spam spam")).thenThrow(new ContentClassifier.Unclassifiable("bad json"));
        when(classifier.classify("POST", "Đồ ngu", "xúc phạm")).thenReturn(Optional.of(
                new AiVerdict("HARASSMENT", 2, "Chửi.", LocalDateTime.now())));

        service.triagePending();

        verify(reportRepository).addAiAttempts(first.getReportId(), 1);
        verify(reportRepository).saveAiVerdict(eq(second.getReportId()), any());
    }

    @Test
    void givesUpAtOnceOnReportsWithoutText() {
        when(moderationService.getTarget(any(), any()))
                .thenReturn(ModerationTargetResponseCommand.missing(ReportTargetType.POST, first.getTargetId()));

        service.triagePending();

        verify(reportRepository).addAiAttempts(first.getReportId(), ReportAiTriageService.MAX_ATTEMPTS);
        verify(reportRepository).addAiAttempts(second.getReportId(), ReportAiTriageService.MAX_ATTEMPTS);
        verifyNoInteractions(classifier);
    }

    private void target(ContentReport report, String content) {
        when(moderationService.getTarget(ReportTargetType.POST, report.getTargetId()))
                .thenReturn(new ModerationTargetResponseCommand(ReportTargetType.POST, report.getTargetId(), true,
                        UUID.randomUUID(), content, "PUBLISHED", 0, null, LocalDateTime.now()));
    }

    private static ContentReport report(String reason) {
        return ContentReport.builder()
                .reportId(UUID.randomUUID())
                .reporterId(UUID.randomUUID())
                .targetType(ReportTargetType.POST)
                .targetId(UUID.randomUUID())
                .reason(reason)
                .status(ReportStatus.PENDING)
                .build();
    }
}
