package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.ModerateContentCommand;
import iuh.fit.socialservice.application.dto.request.ReviewAppealCommand;
import iuh.fit.socialservice.application.dto.request.SubmitAppealCommand;
import iuh.fit.socialservice.application.dto.response.ContentAppealResponseCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.dto.response.ModerationDecisionResponseCommand;
import iuh.fit.socialservice.application.dto.response.ModerationTargetResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.port.persistence.ContentAppealRepository;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.application.port.persistence.ModerationActionRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.domain.entity.ContentAppeal;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.entity.ModerationAction;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostAttachment;
import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.AttachmentType;
import iuh.fit.socialservice.domain.enums.HighlightKind;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SocialEventPublisher eventPublisher;

    private SocialModerationServiceImpl service;

    private final UUID reportId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();
    private final UUID moderatorId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SocialModerationServiceImpl(
                reportRepository, actionRepository, appealRepository, postRepository, commentRepository, messageRepository,
                eventPublisher
        );
        lenient().when(actionRepository.save(any())).thenAnswer(call -> {
            ModerationAction action = call.getArgument(0);
            action.setActionId(UUID.randomUUID());
            return action;
        });
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
    void hideNotifiesAuthorInRealtime() throws Exception {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(publishedPost()));

        service.actOnReport(command(ModerationActionType.HIDE_CONTENT));

        verify(eventPublisher).publish(eq(SocialEventPublisher.MODERATION_DECIDED), eq(authorId), eq(moderatorId),
                any(), anyMap(), anyString());
    }

    @Test
    void dismissDoesNotNotifyAuthor() throws Exception {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(publishedPost()));

        service.actOnReport(command(ModerationActionType.DISMISS_REPORT));

        verify(eventPublisher, never()).publish(any(), any(), any(), any(), any(), any());
    }

    @Test
    void handledReportOnlyAllowsContentStatusChanges() {
        ContentReport resolved = openReport();
        resolved.setStatus(ReportStatus.RESOLVED);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(resolved));

        assertThatThrownBy(() -> service.actOnReport(command(ModerationActionType.WARNING)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("đổi trạng thái");
        verify(actionRepository, never()).save(any());
    }

    @Test
    void handledReportCanRestoreHiddenContentAndKeepsItsStatus() throws Exception {
        ContentReport resolved = openReport();
        resolved.setStatus(ReportStatus.RESOLVED);
        Post hidden = publishedPost();
        hidden.setStatus(PostStatus.HIDDEN);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(resolved));
        when(postRepository.findById(postId)).thenReturn(Optional.of(hidden));

        ContentReportResponseCommand response = service.actOnReport(command(ModerationActionType.RESTORE_CONTENT));

        assertThat(hidden.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(eventPublisher).publish(eq(SocialEventPublisher.MODERATION_DECIDED), eq(authorId), any(), any(),
                anyMap(), anyString());
    }

    @Test
    void statusChangeToCurrentStatusIsRejected() {
        ContentReport resolved = openReport();
        resolved.setStatus(ReportStatus.RESOLVED);
        Post hidden = publishedPost();
        hidden.setStatus(PostStatus.HIDDEN);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(resolved));
        when(postRepository.findById(postId)).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> service.actOnReport(command(ModerationActionType.HIDE_CONTENT)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("trạng thái này");
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
                .hasMessageContaining("người bị xử lý");
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
        ModerationAction action = hideAction(actionId);
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(action));
        when(actionRepository.findByReportId(reportId)).thenReturn(List.of(action));
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
        verify(eventPublisher).publish(eq(SocialEventPublisher.MODERATION_APPEAL_REVIEWED), eq(authorId),
                eq(moderatorId), eq(appealId), anyMap(), anyString());
    }

    @Test
    void warningIsAppealableAndApprovalLeavesContentAlone() throws Exception {
        UUID actionId = UUID.randomUUID();
        UUID appealId = UUID.randomUUID();
        ModerationAction warning = hideAction(actionId);
        warning.setActionType(ModerationActionType.WARNING);
        warning.setContentAuthorId(authorId);
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(warning));
        when(actionRepository.findByReportId(reportId)).thenReturn(List.of(warning));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        when(appealRepository.save(any())).thenAnswer(call -> {
            ContentAppeal appeal = call.getArgument(0);
            if (appeal.getAppealId() == null) appeal.setAppealId(appealId);
            return appeal;
        });

        ContentAppealResponseCommand submitted = service.submitAppeal(SubmitAppealCommand.builder()
                .actionId(actionId).appellantId(authorId).reason("Tôi không hề spam bài viết").build());
        when(appealRepository.findById(appealId)).thenReturn(Optional.of(ContentAppeal.builder()
                .appealId(appealId).actionId(actionId).appellantId(authorId)
                .reason("Tôi không hề spam bài viết").status(AppealStatus.PENDING).build()));
        service.reviewAppeal(ReviewAppealCommand.builder()
                .appealId(appealId).reviewerId(moderatorId).decision(AppealStatus.APPROVED).build());

        assertThat(submitted.getStatus()).isEqualTo(AppealStatus.PENDING);
        verify(postRepository, never()).save(any());
    }

    @Test
    void supersededDecisionCannotBeAppealed() {
        UUID actionId = UUID.randomUUID();
        ModerationAction hide = hideAction(actionId);
        hide.setContentAuthorId(authorId);
        ModerationAction restore = hideAction(UUID.randomUUID());
        restore.setActionType(ModerationActionType.RESTORE_CONTENT);
        when(actionRepository.findById(actionId)).thenReturn(Optional.of(hide));
        when(actionRepository.findByReportId(reportId)).thenReturn(List.of(hide, restore));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));

        assertThatThrownBy(() -> service.submitAppeal(SubmitAppealCommand.builder()
                .actionId(actionId).appellantId(authorId).reason("Nội dung của tôi không vi phạm").build()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("mới hơn");
    }

    @Test
    void myDecisionsShowAppealableLatestAction() throws Exception {
        UUID actionId = UUID.randomUUID();
        ModerationAction hide = hideAction(actionId);
        hide.setContentAuthorId(authorId);
        Page<ModerationAction> page = new PageImpl<>(List.of(hide));
        when(actionRepository.findByContentAuthorId(eq(authorId), any(), any())).thenReturn(page);
        when(actionRepository.findByReportId(reportId)).thenReturn(List.of(hide));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(openReport()));
        Post hidden = publishedPost();
        hidden.setStatus(PostStatus.HIDDEN);
        when(postRepository.findById(postId)).thenReturn(Optional.of(hidden));
        when(appealRepository.findByActionId(actionId)).thenReturn(Optional.empty());

        List<ModerationDecisionResponseCommand> decisions =
                service.getMyDecisions(authorId, PageRequest.of(0, 10)).getContent();

        assertThat(decisions).hasSize(1);
        assertThat(decisions.get(0).appealable()).isTrue();
        assertThat(decisions.get(0).contentStatus()).isEqualTo("HIDDEN");
        assertThat(decisions.get(0).contentSnippet()).isEqualTo("bài viết");
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

    @Test
    void readsReportedPostEvenWhenHidden() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(Post.builder()
                .postId(postId).authorId(authorId).content("Bài quảng cáo").status(PostStatus.HIDDEN)
                .attachments(new ArrayList<>()).build()));

        ModerationTargetResponseCommand target = service.getTarget(ReportTargetType.POST, postId);

        assertThat(target.found()).isTrue();
        assertThat(target.authorId()).isEqualTo(authorId);
        assertThat(target.content()).isEqualTo("Bài quảng cáo");
        assertThat(target.status()).isEqualTo("HIDDEN");
    }

    @Test
    void readsPostWithoutTextIncludingMediaHighlightAndSharedOriginal() {
        UUID originalId = UUID.randomUUID();
        PostHighlight highlight = new PostHighlight(HighlightKind.MATCH, UUID.randomUUID(), "Giao hữu", "Thắng 3-1",
                null, "WIN", List.of(), List.of(), null, null, List.of());
        List<PostAttachment> attachments = new ArrayList<>(List.of(
                PostAttachment.builder().storageKey("social-posts/b.jpg").type(AttachmentType.IMAGE).displayOrder(2).build(),
                PostAttachment.builder().storageKey("social-posts/a.jpg").type(AttachmentType.IMAGE).displayOrder(1).build()));
        when(postRepository.findById(postId)).thenReturn(Optional.of(Post.builder()
                .postId(postId).authorId(authorId).status(PostStatus.PUBLISHED).highlight(highlight)
                .attachments(attachments).sharedPostId(originalId).build()));
        when(postRepository.findById(originalId)).thenReturn(Optional.of(Post.builder()
                .postId(originalId).authorId(UUID.randomUUID()).content("Bài gốc").status(PostStatus.PUBLISHED)
                .build()));

        ModerationTargetResponseCommand target = service.getTarget(ReportTargetType.POST, postId);

        assertThat(target.content()).isNull();
        assertThat(target.attachments()).extracting(ModerationTargetResponseCommand.Attachment::storageKey)
                .containsExactly("social-posts/a.jpg", "social-posts/b.jpg");
        assertThat(target.highlight()).isEqualTo(highlight);
        assertThat(target.shared().content()).isEqualTo("Bài gốc");
    }

    @Test
    void reportsMissingContentInsteadOfFailing() {
        when(commentRepository.findById(postId)).thenReturn(Optional.empty());

        assertThat(service.getTarget(ReportTargetType.COMMENT, postId).found()).isFalse();
        assertThat(service.getTarget(ReportTargetType.VENUE, postId).found()).isFalse();
    }
}
