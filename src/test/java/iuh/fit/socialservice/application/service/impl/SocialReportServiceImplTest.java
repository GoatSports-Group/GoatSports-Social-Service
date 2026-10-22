package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.CreateContentReportCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialReportServiceImplTest {
    @Mock
    private ContentReportRepository reportRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository commentRepository;

    private SocialReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SocialReportServiceImpl(reportRepository, postRepository, commentRepository);
    }

    @Test
    void createReportStartsInPendingState() throws ServiceException {
        UUID reporterId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.of(Post.builder().postId(postId).build()));
        when(reportRepository.existsOpenReport(any(), any(), any(), any())).thenReturn(false);
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(invocation -> {
            ContentReport report = invocation.getArgument(0);
            report.setReportId(UUID.randomUUID());
            return report;
        });

        ContentReportResponseCommand response = service.createReport(CreateContentReportCommand.builder()
                .reporterId(reporterId)
                .targetType(ReportTargetType.POST)
                .targetId(postId)
                .reason("Nội dung có dấu hiệu quảng cáo rác")
                .evidence(List.of())
                .build());

        assertThat(response.getReporterId()).isEqualTo(reporterId);
        assertThat(response.getTargetId()).isEqualTo(postId);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void createReportRejectsDuplicateOpenReport() {
        UUID reporterId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.of(Post.builder().postId(postId).build()));
        when(reportRepository.existsOpenReport(any(), any(), any(), any())).thenReturn(true);

        CreateContentReportCommand command = CreateContentReportCommand.builder()
                .reporterId(reporterId)
                .targetType(ReportTargetType.POST)
                .targetId(postId)
                .reason("Nội dung vi phạm tiêu khoản cộng đồng")
                .evidence(List.of())
                .build();

        assertThatThrownBy(() -> service.createReport(command))
                .isInstanceOf(ServiceException.class);
        verify(reportRepository, never()).save(any());
    }
}
