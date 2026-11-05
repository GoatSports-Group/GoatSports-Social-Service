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
import iuh.fit.socialservice.application.service.SocialModerationService;
import iuh.fit.socialservice.domain.entity.ContentAppeal;
import iuh.fit.socialservice.domain.entity.ContentReport;
import iuh.fit.socialservice.domain.entity.Message;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialModerationServiceImpl implements SocialModerationService {
    private static final Set<ReportStatus> OPEN_STATUSES = Set.of(ReportStatus.PENDING, ReportStatus.REVIEWING);
    /** Quyết định bất lợi cho người bị xử lý: được khiếu nại. */
    private static final Set<ModerationActionType> APPEALABLE = Set.of(
            ModerationActionType.HIDE_CONTENT, ModerationActionType.REMOVE_CONTENT, ModerationActionType.WARNING);
    /** Đổi trạng thái nội dung: làm được cả khi báo cáo đã đóng. */
    private static final Set<ModerationActionType> STATUS_CHANGES = Set.of(
            ModerationActionType.HIDE_CONTENT, ModerationActionType.REMOVE_CONTENT, ModerationActionType.RESTORE_CONTENT);
    /** Quyết định chạm tới người bị báo cáo: họ được báo và thấy trong danh sách của mình. Bác báo cáo thì không. */
    private static final Set<ModerationActionType> AUTHOR_FACING = Set.of(
            ModerationActionType.HIDE_CONTENT, ModerationActionType.REMOVE_CONTENT, ModerationActionType.WARNING,
            ModerationActionType.RESTORE_CONTENT);
    private static final int SNIPPET_LENGTH = 140;
    private static final int NOTIFY_REASON_LENGTH = 200;

    private final ContentReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final ContentAppealRepository appealRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final MessageRepository messageRepository;
    private final SocialEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public ModerationTargetResponseCommand getTarget(ReportTargetType targetType, UUID targetId) {
        if (targetType == null || targetId == null) return ModerationTargetResponseCommand.missing(targetType, targetId);
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(post -> postTarget(post, true))
                    .orElseGet(() -> ModerationTargetResponseCommand.missing(targetType, targetId));
            case COMMENT -> commentRepository.findById(targetId)
                    .map(comment -> new ModerationTargetResponseCommand(targetType, targetId, true, comment.getAuthorId(),
                            comment.getContent(), String.valueOf(comment.getStatus()), 0, comment.getPostId(),
                            comment.getCreatedAt()))
                    .orElseGet(() -> ModerationTargetResponseCommand.missing(targetType, targetId));
            case MESSAGE -> messageRepository.findById(targetId)
                    .map(this::messageTarget)
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

    /**
     * Báo cáo còn mở: mọi hành động, đóng báo cáo. Báo cáo đã đóng: chỉ đổi trạng thái nội dung (ẩn / gỡ / khôi phục)
     * sang trạng thái khác hiện tại; báo cáo giữ nguyên trạng thái, quyết định mới nối vào lịch sử.
     */
    @Override
    @Transactional
    public ContentReportResponseCommand actOnReport(ModerateContentCommand command) throws ServiceException {
        String reason = command.validate();
        ContentReport report = requireReport(command.getReportId());
        ModerationActionType actionType = command.getActionType();
        boolean open = OPEN_STATUSES.contains(report.getStatus());
        if (!open) requireStatusChange(report, actionType);

        PostStatus targetStatus = contentStatusFor(actionType);
        if (targetStatus != null) {
            applyContentStatus(report.getTargetType(), report.getTargetId(), targetStatus);
        }

        ModerationTargetResponseCommand target = getTarget(report.getTargetType(), report.getTargetId());
        ModerationAction action = actionRepository.save(ModerationAction.builder()
                .reportId(report.getReportId())
                .moderatorId(command.getModeratorId())
                .contentAuthorId(target.found() ? target.authorId() : null)
                .actionType(actionType)
                .reason(reason)
                .actionAt(LocalDateTime.now())
                .build());

        if (open) {
            report.setStatus(actionType == ModerationActionType.DISMISS_REPORT
                    ? ReportStatus.REJECTED
                    : ReportStatus.RESOLVED);
        }
        ContentReport saved = reportRepository.save(report);
        notifyDecision(action, report.getTargetType());
        return toResponse(saved);
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
        UUID authorId = action.getContentAuthorId() != null
                ? action.getContentAuthorId()
                : contentAuthorId(report.getTargetType(), report.getTargetId());
        if (!command.getAppellantId().equals(authorId)) {
            throw new ServiceException("Chỉ người bị xử lý mới được khiếu nại");
        }
        if (!isLatest(action)) {
            throw new ServiceException("Quyết định này đã được thay bằng một quyết định mới hơn");
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

        // Cảnh cáo không đổi trạng thái nội dung: chấp nhận khiếu nại chỉ là rút lại cảnh cáo.
        if (command.getDecision() == AppealStatus.APPROVED && contentStatusFor(action.getActionType()) != null) {
            applyContentStatus(report.getTargetType(), report.getTargetId(), PostStatus.PUBLISHED);
        }

        appeal.setStatus(command.getDecision());
        appeal.setReviewedBy(command.getReviewerId());
        appeal.setReviewedAt(LocalDateTime.now());
        ContentAppeal saved = appealRepository.save(appeal);
        eventPublisher.publish(SocialEventPublisher.MODERATION_APPEAL_REVIEWED, saved.getAppellantId(),
                command.getReviewerId(), saved.getAppealId(),
                Map.of("decision", saved.getStatus().name(),
                        "actionType", action.getActionType().name(),
                        "targetType", report.getTargetType().name()),
                saved.getAppealId().toString());
        return toResponse(saved, action, report);
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

    @Override
    @Transactional(readOnly = true)
    public Page<ModerationDecisionResponseCommand> getMyDecisions(UUID authorId, Pageable pageable)
            throws ServiceException {
        if (authorId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        // ponytail: vài truy vấn cho mỗi quyết định; trang chỉ 10 dòng nên chưa cần gộp.
        return actionRepository.findByContentAuthorId(authorId, AUTHOR_FACING, pageable).map(this::toDecision);
    }

    private ModerationDecisionResponseCommand toDecision(ModerationAction action) {
        Optional<ContentReport> report = reportRepository.findById(action.getReportId());
        ModerationTargetResponseCommand target = report
                .map(value -> getTarget(value.getTargetType(), value.getTargetId()))
                .orElse(null);
        Optional<ContentAppeal> appeal = appealRepository.findByActionId(action.getActionId());
        boolean appealable = APPEALABLE.contains(action.getActionType()) && appeal.isEmpty() && isLatest(action);
        boolean found = target != null && target.found();
        return new ModerationDecisionResponseCommand(
                action.getActionId(),
                action.getActionType(),
                action.getReason(),
                action.getActionAt(),
                report.map(ContentReport::getTargetType).orElse(null),
                report.map(ContentReport::getTargetId).orElse(null),
                found ? snippet(target.content(), SNIPPET_LENGTH) : null,
                found ? target.status() : null,
                appealable,
                appeal.map(value -> new ModerationDecisionResponseCommand.Appeal(value.getAppealId(), value.getStatus(),
                        value.getReason(), value.getCreatedAt(), value.getReviewedAt())).orElse(null));
    }

    private ModerationTargetResponseCommand postTarget(Post post, boolean withShared) {
        List<ModerationTargetResponseCommand.Attachment> attachments = post.getAttachments() == null
                ? List.of()
                : post.getAttachments().stream()
                        .sorted(Comparator.comparingInt(attachment ->
                                attachment.getDisplayOrder() == null ? 0 : attachment.getDisplayOrder()))
                        .map(attachment -> new ModerationTargetResponseCommand.Attachment(
                                attachment.getStorageKey(), attachment.getType(), null))
                        .toList();
        ModerationTargetResponseCommand shared = null;
        if (withShared && post.getSharedPostId() != null) {
            shared = postRepository.findById(post.getSharedPostId())
                    .map(original -> postTarget(original, false))
                    .orElseGet(() -> ModerationTargetResponseCommand.missing(ReportTargetType.POST, post.getSharedPostId()));
        }
        return new ModerationTargetResponseCommand(ReportTargetType.POST, post.getPostId(), true, post.getAuthorId(),
                post.getContent(), String.valueOf(post.getStatus()), attachments.size(), null, post.getCreatedAt(),
                attachments, post.getSport() == null ? null : post.getSport().name(), post.getHighlight(),
                post.getPlayerCall(), shared);
    }

    private ModerationTargetResponseCommand messageTarget(Message message) {
        List<ModerationTargetResponseCommand.Attachment> attachments = message.getAttachments() == null
                ? List.of()
                : message.getAttachments().stream()
                        .map(attachment -> new ModerationTargetResponseCommand.Attachment(
                                attachment.getStorageKey(), attachment.getType(), attachment.getFileName()))
                        .toList();
        String status = Boolean.TRUE.equals(message.getDeleted()) ? "DELETED" : String.valueOf(message.getStatus());
        return new ModerationTargetResponseCommand(ReportTargetType.MESSAGE, message.getMessageId(), true,
                message.getSenderId(), message.getContent(), status, attachments.size(), null, message.getSentAt(),
                attachments, null, null, null, null);
    }

    private void requireStatusChange(ContentReport report, ModerationActionType actionType) throws ServiceException {
        if (!STATUS_CHANGES.contains(actionType)) {
            throw new ServiceException("Báo cáo đã xử lý chỉ có thể đổi trạng thái nội dung (ẩn, gỡ hoặc khôi phục)");
        }
        if (report.getTargetType() != ReportTargetType.POST && report.getTargetType() != ReportTargetType.COMMENT) {
            throw new ServiceException("Chỉ đổi được trạng thái của bài viết và bình luận");
        }
        ModerationTargetResponseCommand target = getTarget(report.getTargetType(), report.getTargetId());
        if (!target.found()) throw new ServiceException("Nội dung không còn trên hệ thống");
        if (contentStatusFor(actionType).name().equals(target.status())) {
            throw new ServiceException("Nội dung đang ở trạng thái này rồi");
        }
    }

    /** Báo người bị xử lý (realtime qua notification-service); admin tự xử lý nội dung của mình thì không báo. */
    private void notifyDecision(ModerationAction action, ReportTargetType targetType) {
        UUID authorId = action.getContentAuthorId();
        if (authorId == null || authorId.equals(action.getModeratorId())
                || !AUTHOR_FACING.contains(action.getActionType())) {
            return;
        }
        eventPublisher.publish(SocialEventPublisher.MODERATION_DECIDED, authorId, action.getModeratorId(),
                action.getActionId(),
                Map.of("actionType", action.getActionType().name(),
                        "targetType", targetType.name(),
                        "reason", snippet(action.getReason(), NOTIFY_REASON_LENGTH),
                        "appealable", String.valueOf(APPEALABLE.contains(action.getActionType()))),
                action.getActionId().toString());
    }

    /** Một báo cáo có thể bị xử lý nhiều lần (đổi trạng thái); chỉ quyết định mới nhất còn hiệu lực. */
    private boolean isLatest(ModerationAction action) {
        List<ModerationAction> actions = actionRepository.findByReportId(action.getReportId());
        return !actions.isEmpty() && actions.get(actions.size() - 1).getActionId().equals(action.getActionId());
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

    /** Quyết định cũ chưa lưu tác giả: tra lại từ nội dung (bài, bình luận, tin nhắn, tài khoản). */
    private UUID contentAuthorId(ReportTargetType targetType, UUID targetId) throws ServiceException {
        ModerationTargetResponseCommand target = getTarget(targetType, targetId);
        if (!target.found() || target.authorId() == null) {
            throw new ServiceException("Không tìm thấy nội dung liên quan");
        }
        return target.authorId();
    }

    private static String snippet(String value, int maxLength) {
        if (value == null) return "";
        String text = value.strip();
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
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
