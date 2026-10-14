package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.ModerateContentCommand;
import iuh.fit.socialservice.application.dto.request.ReviewAppealCommand;
import iuh.fit.socialservice.application.dto.request.SubmitAppealCommand;
import iuh.fit.socialservice.application.dto.response.ContentAppealResponseCommand;
import iuh.fit.socialservice.application.dto.response.ContentReportResponseCommand;
import iuh.fit.socialservice.application.dto.response.ModerationTargetResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.ContentAppealRepository;
import iuh.fit.socialservice.application.port.persistence.ContentReportRepository;
import iuh.fit.socialservice.application.port.persistence.MessageRepository;
import iuh.fit.socialservice.application.port.persistence.ModerationActionRepository;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.service.SocialModerationService;
import iuh.fit.socialservice.domain.entity.ContentAppeal;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.entity.ModerationAction;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.domain.enums.ModerationActionType;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.domain.enums.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialModerationServiceImpl implements SocialModerationService {
    private static final Set<ReportStatus> OPEN_STATUSES = Set.of(ReportStatus.PENDING, ReportStatus.REVIEWING);
    private static final Set<ModerationActionType> APPEALABLE =
            Set.of(ModerationActionType.HIDE_CONTENT, ModerationActionType.REMOVE_CONTENT);

    private final ContentReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final ContentAppealRepository appealRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional(readOnly = true)
    public ModerationTargetResponseCommand getTarget(ReportTargetType targetType, UUID targetId) {
        if (targetType == null || targetId == null) return ModerationTargetResponseCommand.missing(targetType, targetId);
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(post -> new ModerationTargetResponseCommand(targetType, targetId, true, post.getAuthorId(),
                            post.getContent(), String.valueOf(post.getStatus()),
                            post.getAttachments() == null ? 0 : post.getAttachments().size(), null, post.getCreatedAt()))
                    .orElseGet(() -> ModerationTargetResponseCommand.missing(targetType, targetId));
            case COMMENT -> commentRepository.findById(targetId)
                    .map(comment -> new ModerationTargetResponseCommand(targetType, targetId, true, comment.getAuthorId(),
                            comment.getContent(), String.valueOf(comment.getStatus()), 0, comment.getPostId(),
                            comment.getCreatedAt()))
                    .orElseGet(() -> ModerationTargetResponseCommand.missing(targetType, targetId));
            case MESSAGE -> messageRepository.findById(targetId)
                    .map(message -> new ModerationTargetResponseCommand(targetType, targetId, true, message.getSenderId(),
                            message.getContent(), Boolean.TRUE.equals(message.getDeleted()) ? "DELETED" : String.valueOf(message.getStatus()),
                            message.getAttachments() == null ? 0 : message.getAttachments().size(), null, message.getSentAt()))
                    .orElseGet(() -> ModerationTargetResponseCommand.missing(targetType, targetId));
            // Báo cáo một tài khoản: "tác giả" chính là tài khoản đó, không có nội dung riêng.
            case USER -> new ModerationTargetResponseCommand(targetType, targetId, true, targetId, null, null, 0, null, null);
            default -> ModerationTargetResponseCommand.missing(targetType, targetId);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentReportResponseCommand> getReportQueue(
            Collection<ReportStatus> statuses,
            Pageable pageable
    ) {
        Collection<ReportStatus> effective = statuses == null || statuses.isEmpty() ? OPEN_STATUSES : statuses;
        // ponytail: N+1 khi nạp action cho từng report - chap nhan vi trang admin chi vai chuc dong.
        return reportRepository.findQueue(effective, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentReportResponseCommand getReport(UUID reportId) throws ServiceException {
        return toResponse(requireReport(reportId));
    }

    @Override
    @Transactional
    public ContentReportResponseCommand actOnReport(ModerateContentCommand command) throws ServiceException {
        String reason = command.validate();
        ContentReport report = requireReport(command.getReportId());
        if (!OPEN_STATUSES.contains(report.getStatus())) {
            throw new ServiceException("Báo cáo này đã được xử lý");
        }

        ModerationActionType actionType = command.getActionType();
        PostStatus targetStatus = contentStatusFor(actionType);
        if (targetStatus != null) {
            applyContentStatus(report.getTargetType(), report.getTargetId(), targetStatus);
        }

        actionRepository.save(ModerationAction.builder()
                .reportId(report.getReportId())
                .moderatorId(command.getModeratorId())
                .actionType(actionType)
                .reason(reason)
                .actionAt(LocalDateTime.now())
                .build());

        report.setStatus(actionType == ModerationActionType.DISMISS_REPORT
                ? ReportStatus.REJECTED
                : ReportStatus.RESOLVED);
        return toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public ContentAppealResponseCommand submitAppeal(SubmitAppealCommand command) throws ServiceException {
        String reason = command.validate();
        ModerationAction action = actionRepository.findById(command.getActionId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy quyết định kiểm duyệt"));
        if (!APPEALABLE.contains(action.getActionType())) {
            throw new ServiceException("Quyết định này không thuộc diện được khiếu nại");
        }
        ContentReport report = requireReport(action.getReportId());
        if (!command.getAppellantId().equals(contentAuthorId(report.getTargetType(), report.getTargetId()))) {
            throw new ServiceException("Chỉ tác giả nội dung mới được khiếu nại");
        }
        if (appealRepository.existsByActionId(action.getActionId())) {
            throw new ServiceException("Quyết định này đã được khiếu nại");
        }

        ContentAppeal appeal = appealRepository.save(ContentAppeal.builder()
                .actionId(action.getActionId())
                .appellantId(command.getAppellantId())
                .reason(reason)
                .status(AppealStatus.PENDING)
                .build());
        return toResponse(appeal, action, report);
    }

    @Override
    @Transactional
    public ContentAppealResponseCommand reviewAppeal(ReviewAppealCommand command) throws ServiceException {
        command.validate();
        ContentAppeal appeal = appealRepository.findById(command.getAppealId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy khiếu nại"));
        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ServiceException("Khiếu nại này đã được xử lý");
        }
        ModerationAction action = actionRepository.findById(appeal.getActionId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy quyết định kiểm duyệt"));
        ContentReport report = requireReport(action.getReportId());

        if (command.getDecision() == AppealStatus.APPROVED) {
            applyContentStatus(report.getTargetType(), report.getTargetId(), PostStatus.PUBLISHED);
        }

        appeal.setStatus(command.getDecision());
        appeal.setReviewedBy(command.getReviewerId());
        appeal.setReviewedAt(LocalDateTime.now());
        return toResponse(appealRepository.save(appeal), action, report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentAppealResponseCommand> getAppealQueue(
            Collection<AppealStatus> statuses,
            Pageable pageable
    ) {
        Collection<AppealStatus> effective =
                statuses == null || statuses.isEmpty() ? Set.of(AppealStatus.PENDING) : statuses;
        return appealRepository.findByStatusIn(effective, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentAppealResponseCommand> getMyAppeals(UUID appellantId, Pageable pageable)
            throws ServiceException {
        if (appellantId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        return appealRepository.findByAppellantId(appellantId, pageable).map(this::toResponse);
    }

    private PostStatus contentStatusFor(ModerationActionType actionType) throws ServiceException {
        return switch (actionType) {
            case HIDE_CONTENT -> PostStatus.HIDDEN;
            case REMOVE_CONTENT -> PostStatus.REMOVED;
            case RESTORE_CONTENT -> PostStatus.PUBLISHED;
            case WARNING, DISMISS_REPORT -> null;
            // Social khong so huu User/Venue (muc 1 implementation plan) nen khong the tu dinh chi.
            case SUSPEND_USER, SUSPEND_VENUE ->
                    throw new ServiceException("Đình chỉ tài khoản hoặc sân phải do dịch vụ sở hữu thực hiện");
        };
    }

    private void applyContentStatus(ReportTargetType targetType, UUID targetId, PostStatus status)
            throws ServiceException {
        if (targetType == ReportTargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết cần xử lý"));
            if (post.getStatus() == status) return;
            post.setStatus(status);
            post.setUpdatedAt(LocalDateTime.now());
            postRepository.save(post);
            return;
        }
        if (targetType == ReportTargetType.COMMENT) {
            PostComment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy bình luận cần xử lý"));
            if (comment.getStatus() == status) return;
            comment.setStatus(status);
            comment.setUpdatedAt(LocalDateTime.now());
            commentRepository.save(comment);
            return;
        }
        throw new ServiceException("Loại nội dung này chưa được hỗ trợ kiểm duyệt");
    }

    private UUID contentAuthorId(ReportTargetType targetType, UUID targetId) throws ServiceException {
        if (targetType == ReportTargetType.POST) {
            return postRepository.findById(targetId)
                    .map(Post::getAuthorId)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết liên quan"));
        }
        if (targetType == ReportTargetType.COMMENT) {
            return commentRepository.findById(targetId)
                    .map(PostComment::getAuthorId)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy bình luận liên quan"));
        }
        throw new ServiceException("Loại nội dung này chưa được hỗ trợ khiếu nại");
    }

    private ContentReport requireReport(UUID reportId) throws ServiceException {
        if (reportId == null) throw new ServiceException("Báo cáo không hợp lệ");
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy báo cáo"));
    }

    private ContentReportResponseCommand toResponse(ContentReport report) {
        List<ContentReportResponseCommand.ActionItem> actions =
                actionRepository.findByReportId(report.getReportId()).stream()
                        .map(action -> ContentReportResponseCommand.ActionItem.builder()
                                .actionId(action.getActionId())
                                .moderatorId(action.getModeratorId())
                                .actionType(action.getActionType())
                                .reason(action.getReason())
                                .actionAt(action.getActionAt())
                                .build())
                        .toList();
        return ContentReportResponseCommand.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporterId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .evidence(report.getEvidence())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .actions(actions)
                .ai(report.getAiVerdict())
                .build();
    }

    private ContentAppealResponseCommand toResponse(ContentAppeal appeal) {
        Optional<ModerationAction> action = actionRepository.findById(appeal.getActionId());
        Optional<ContentReport> report = action.flatMap(value -> reportRepository.findById(value.getReportId()));
        return ContentAppealResponseCommand.builder()
                .appealId(appeal.getAppealId())
                .actionId(appeal.getActionId())
                .appellantId(appeal.getAppellantId())
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .reviewedBy(appeal.getReviewedBy())
                .reviewedAt(appeal.getReviewedAt())
                .createdAt(appeal.getCreatedAt())
                .actionType(action.map(ModerationAction::getActionType).orElse(null))
                .targetType(report.map(ContentReport::getTargetType).orElse(null))
                .targetId(report.map(ContentReport::getTargetId).orElse(null))
                .build();
    }

    private ContentAppealResponseCommand toResponse(
            ContentAppeal appeal,
            ModerationAction action,
            ContentReport report
    ) {
        return ContentAppealResponseCommand.builder()
                .appealId(appeal.getAppealId())
                .actionId(appeal.getActionId())
                .appellantId(appeal.getAppellantId())
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .reviewedBy(appeal.getReviewedBy())
                .reviewedAt(appeal.getReviewedAt())
                .createdAt(appeal.getCreatedAt())
                .actionType(action.getActionType())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .build();
    }
}
