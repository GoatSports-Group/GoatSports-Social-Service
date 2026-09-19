package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.ModerateContentCommand;
import iuh.fit.socialservice.application.dto.request.ReviewAppealCommand;
import iuh.fit.socialservice.application.dto.request.SubmitAppealCommand;
import iuh.fit.socialservice.application.dto.response.ContentAppealResponseCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ContentAppealRepository;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.port.persistence.ModerationActionRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.ContentAppeal;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.entity.ModerationAction;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.ModerationActionType;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialModerationServiceImplTest {
    @Mock
    private ContentReportRepository reportRepository;
    @Mock
    private ModerationActionRepository actionRepository;
    @Mock
    private ContentAppealRepository appealRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository commentRepository;

    private SocialModerationServiceImpl service;

    private final UUID reportId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();
    private final UUID moderatorId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SocialModerationServiceImpl(
                reportRepository, actionRepository, appealRepository, postRepository, commentRepository
        );
        lenient().when(actionRepository.findByReportId(any())).thenReturn(List.of());
        lenient().when(reportRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private ContentReport openReport() {
        return ContentReport.builder()
                .reportId(reportId)
                .reporterId(UUID.randomUUID())
                .targetType(ReportTargetType.POST)
                .targetId(postId)
                .reason("Nội dung phản cảm")
                .evidence(List.of())
                .status(ReportStatus.PENDING)
                .build();
    }

    private Post publishedPost() {
        return Post.builder()
                .postId(postId)
                .authorId(authorId)
                .content("bài viết")
                .status(PostStatus.PUBLISHED)
                .build();
    }

    private ModerateContentCommand command(ModerationActionType actionType) {
        return ModerateContentCommand.builder()
                .reportId(reportId)
                .moderatorId(moderatorId)
                .actionType(actionType)
                .reason("Vi phạm tiêu chuẩn cộng đồng")
                .build();
    }

    @Test
    void hideContentMarksPostHiddenAndResolvesReport() throws Exception {
        Post post = publishedPost();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        ContentReportResponseCommand response = service.actOnReport(command(ModerationActionType.HIDE_CONTENT));

        assertThat(post.getStatus()).isEqualTo(PostStatus.HIDDEN);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(postRepository).save(post);
        verify(actionRepository).save(any(ModerationAction.class));
    }

    @Test
    void dismissReportRejectsItWithoutTouchingContent() throws Exception {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));

        ContentReportResponseCommand response = service.actOnReport(command(ModerationActionType.DISMISS_REPORT));

        assertThat(response.getStatus()).isEqualTo(ReportStatus.REJECTED);
        verify(postRepository, never()).save(any());
        verify(actionRepository).save(any(ModerationAction.class));
    }

    @Test
    void alreadyHandledReportCannotBeActedOnAgain() {
        ContentReport resolved = openReport();
        resolved.setStatus(ReportStatus.RESOLVED);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(resolved));

        assertThatThrownBy(() -> service.actOnReport(command(ModerationActionType.REMOVE_CONTENT)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("đã được xử lý");
        verify(actionRepository, never()).save(any());
    }

    @Test
    void suspendUserIsRejectedBecauseSocialDoesNotOwnAccounts() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));

        assertThatThrownBy(() -> service.actOnReport(command(ModerationActionType.SUSPEND_USER)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("dịch vụ sở hữu");
        verify(actionRepository, never()).save(any());
    }

    private ModerationAction hideAction(UUID actionId) {
        return ModerationAction.builder()
                .actionId(actionId)
                .reportId(reportId)
                .moderatorId(moderatorId)
                .actionType(ModerationActionType.HIDE_CONTENT)
                .reason("Vi phạm tiêu chuẩn cộng đồng")
                .actionAt(LocalDateTime.now())
                .build();
    }

    @Test
    void onlyContentAuthorCanAppeal() {
        UUID actionId = UUID.randomUUID();
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(hideAction(actionId)));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(publishedPost()));

        SubmitAppealCommand command = SubmitAppealCommand.builder()
                .actionId(actionId)
                .appellantId(UUID.randomUUID())
                .reason("Tôi không vi phạm gì cả")
                .build();

        assertThatThrownBy(() -> service.submitAppeal(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("tác giả nội dung");
        verify(appealRepository, never()).save(any());
    }

    @Test
    void dismissActionIsNotAppealable() {
        UUID actionId = UUID.randomUUID();
        ModerationAction dismissed = hideAction(actionId);
        dismissed.setActionType(ModerationActionType.DISMISS_REPORT);
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(dismissed));

        SubmitAppealCommand command = SubmitAppealCommand.builder()
                .actionId(actionId)
                .appellantId(authorId)
                .reason("Tôi muốn khiếu nại quyết định này")
                .build();

        assertThatThrownBy(() -> service.submitAppeal(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("không thuộc diện được khiếu nại");
    }

    @Test
    void authorAppealIsStoredAsPending() throws Exception {
        UUID actionId = UUID.randomUUID();
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(hideAction(actionId)));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(publishedPost()));
        when(appealRepository.existsByActionId(actionId)).thenReturn(false);
        when(appealRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        ContentAppealResponseCommand response = service.submitAppeal(SubmitAppealCommand.builder()
                .actionId(actionId)
                .appellantId(authorId)
                .reason("Nội dung của tôi không vi phạm")
                .build());

        ArgumentCaptor<ContentAppeal> captor = ArgumentCaptor.forClass(ContentAppeal.class);
        verify(appealRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AppealStatus.PENDING);
        assertThat(response.getActionType()).isEqualTo(ModerationActionType.HIDE_CONTENT);
    }

    @Test
    void approvedAppealRestoresHiddenContent() throws Exception {
        UUID actionId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        Post hidden = publishedPost();
        hidden.setStatus(PostStatus.HIDDEN);

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(ContentAppeal.builder()
                .appealId(appealId)
                .actionId(actionId)
                .appellantId(authorId)
                .reason("Nội dung của tôi không vi phạm")
                .status(AppealStatus.PENDING)
                .build()));
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(hideAction(actionId)));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(hidden));
        when(appealRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        ContentAppealResponseCommand response = service.reviewAppeal(ReviewAppealCommand.builder()
                .appealId(appealId)
                .reviewerId(moderatorId)
                .decision(AppealStatus.APPROVED)
                .build());

        assertThat(hidden.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(response.getStatus()).isEqualTo(AppealStatus.APPROVED);
        verify(postRepository).save(hidden);
    }

    @Test
    void rejectedAppealLeavesContentHidden() throws Exception {
        UUID actionId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(ContentAppeal.builder()
                .appealId(appealId)
                .actionId(actionId)
                .appellantId(authorId)
                .reason("Nội dung của tôi không vi phạm")
                .status(AppealStatus.PENDING)
                .build()));
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(hideAction(actionId)));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(appealRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        ContentAppealResponseCommand response = service.reviewAppeal(ReviewAppealCommand.builder()
                .appealId(appealId)
                .reviewerId(moderatorId)
                .decision(AppealStatus.REJECTED)
                .build());

        assertThat(response.getStatus()).isEqualTo(AppealStatus.REJECTED);
        verify(postRepository, never()).save(any());
    }

    @Test
    void appealCanOnlyBeReviewedOnce() {
        UUID appealId = UUID.randomUUID();
        when(appealRepository.findById(appealId)).thenReturn(Optional.of(ContentAppeal.builder()
                .appealId(appealId)
                .actionId(UUID.randomUUID())
                .appellantId(authorId)
                .reason("Nội dung của tôi không vi phạm")
                .status(AppealStatus.APPROVED)
                .build()));

        assertThatThrownBy(() -> service.reviewAppeal(ReviewAppealCommand.builder()
                .appealId(appealId)
                .reviewerId(moderatorId)
                .decision(AppealStatus.REJECTED)
                .build()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("đã được xử lý");
    }
}
