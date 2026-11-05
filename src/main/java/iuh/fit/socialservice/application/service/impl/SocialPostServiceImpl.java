package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.domain.enums.HighlightKind;
import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.application.dto.request.PublishHighlightCommand;
import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostCommentResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.port.external.ClubDirectory;
import iuh.fit.socialservice.application.port.external.ActivityEventPublisher;
import iuh.fit.socialservice.application.port.external.PostRanker;
import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import iuh.fit.socialservice.application.port.external.StorageKeyPromoter;
import iuh.fit.socialservice.application.port.persistence.PostCommentRepository;
import iuh.fit.socialservice.application.port.persistence.PostReactionRepository;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.service.SocialPostService;
import iuh.fit.socialservice.application.service.impl.PostViewAssembler.Viewer;
import iuh.fit.socialservice.domain.entity.Mention;
import iuh.fit.socialservice.domain.entity.PlayerCall;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostAttachment;
import iuh.fit.socialservice.domain.entity.PostComment;
import iuh.fit.socialservice.domain.entity.PostReaction;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.domain.enums.PostStatus;
import iuh.fit.socialservice.domain.enums.PostVisibility;
import iuh.fit.socialservice.domain.enums.ReactionType;
import iuh.fit.socialservice.domain.service.PostMentions;
import iuh.fit.socialservice.domain.service.PostTags;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialPostServiceImpl implements SocialPostService {
    private static final int TRENDING_DAYS = 7;
    /** Goi y khi go "#": nhin rong hon (30 ngay) de co du tu dung chung. */
    private static final int SUGGEST_DAYS = 30;

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final PostViewAssembler assembler;
    private final StorageKeyPromoter storageKeyPromoter;
    private final SocialEventPublisher eventPublisher;
    private final ClubDirectory clubDirectory;
    private final ActivityEventPublisher activityPublisher;
    private final PostRanker postRanker;

    /** Tab Khám phá: chỉ chấm điểm và sắp lại 100 bài mới nhất; sau đó là thứ tự mới nhất như cũ. */
    static final int RANKED_WINDOW = 100;

    private static final int MAX_HIGHLIGHT_CONTENT = 2000;
    private static final Sort CALL_ORDER = Sort.by(
            Sort.Order.asc("playerCall.playDate"),
            Sort.Order.asc("playerCall.startTime"),
            Sort.Order.desc("publishedAt"));

    // ---- reads ---------------------------------------------------------------------------------

    @Override
    public Page<PostResponseCommand> getFeed(UUID viewerId, FeedFilter filter, Pageable pageable) {
        Viewer viewer = assembler.viewer(viewerId);
        if (viewer.id() != null && isExplore(filter)) {
            return rankedExplore(viewer, filter, pageable);
        }
        // Keo tim nguoi: keo sap dien ra dung truoc (ngay, gio choi), khong theo luc dang.
        Pageable order = filter.playerCallsOnly()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), CALL_ORDER)
                : pageable;
        return page(postRepository.findFeed(feedQuery(viewer, filter), order), viewer);
    }

    @Override
    public Page<PostResponseCommand> getSavedPosts(UUID viewerId, Pageable pageable) throws ServiceException {
        Viewer viewer = assembler.viewer(requireActor(viewerId));
        return page(postRepository.findSaved(feedQuery(viewer, new FeedFilter(false, null, null, null)), pageable), viewer);
    }

    @Override
    public List<PostResponseCommand> getOpenPlayerCalls(UUID viewerId, boolean club, PostSport sport, int limit) {
        Viewer viewer = assembler.viewer(viewerId);
        PostRepository.FeedQuery base = feedQuery(viewer, new FeedFilter(false, sport, null, null));
        PostRepository.FeedQuery query = new PostRepository.FeedQuery(base.viewerId(), base.friendIds(),
                base.followingIds(), base.blockedUserIds(), false, sport, null, null,
                new PostRepository.PlayerCallFilter(club, true));
        Pageable page = PageRequest.of(0, Math.max(1, Math.min(limit, 24)),
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        return assembler.toResponses(postRepository.findFeed(query, page).getContent(), viewer);
    }

    @Override
    public PostResponseCommand setPlayerCallFilled(UUID postId, UUID actorId, boolean filled) throws ServiceException {
        Post post = ownedPost(postId, requireActor(actorId));
        if (post.getStatus() != PostStatus.PUBLISHED || post.getPlayerCall() == null) {
            throw new ServiceException("Bài viết không phải kèo tìm người chơi");
        }
        post.setPlayerCall(post.getPlayerCall().withFilled(filled));
        post.setUpdatedAt(LocalDateTime.now());
        return assembler.toResponse(postRepository.save(post), assembler.viewer(actorId));
    }

    @Override
    public AuthorStats getAuthorStats(UUID authorId, UUID viewerId) {
        long total = postRepository
                .findFeed(feedQuery(assembler.viewer(viewerId), new FeedFilter(false, null, authorId, null)),
                        PageRequest.of(0, 1))
                .getTotalElements();
        return new AuthorStats(authorId, total);
    }

    @Override
    public List<PostRepository.TagCount> getTrendingTags(int limit, Set<PostSport> sports) {
        int size = Math.max(1, Math.min(limit, 20));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(TRENDING_DAYS);
        LocalDateTime rising = now.minusDays(1);
        List<PostRepository.TagCount> result = new ArrayList<>();
        if (sports != null && !sports.isEmpty()) result.addAll(postRepository.findTrendingTags(since, rising, sports, size));
        if (result.size() < size) {
            Set<String> seen = new HashSet<>();
            result.forEach(item -> seen.add(PostTags.key(item.tag())));
            postRepository.findTrendingTags(since, rising, Set.of(), size + seen.size()).stream()
                    .filter(item -> !seen.contains(PostTags.key(item.tag())))
                    .limit(size - result.size())
                    .forEach(result::add);
        }
        return result;
    }

    @Override
    public List<PostRepository.TagCount> suggestTags(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return postRepository.findTrendingTags(now.minusDays(SUGGEST_DAYS), now.minusDays(1), Set.of(),
                Math.max(1, Math.min(limit, 50)));
    }

    @Override
    public PostResponseCommand getPost(UUID postId, UUID viewerId) throws ServiceException {
        Viewer viewer = assembler.viewer(viewerId);
        Post post = publishedPost(postId);
        ensureVisible(post, viewer);
        return assembler.toResponse(post, viewer);
    }

    // ---- posts ---------------------------------------------------------------------------------

    @Override
    public PostResponseCommand createPost(SavePostCommand command) throws ServiceException {
        UUID actorId = requireActor(command.getActorId());
        command.validate();
        Viewer actor = assembler.viewer(actorId);
        LocalDateTime now = LocalDateTime.now();
        String content = normalizeContent(command.getContent());
        PlayerCall playerCall = playerCall(command.getPlayerCall(), actorId, null);
        Post post = postRepository.save(Post.builder()
                .authorId(actorId)
                .content(content)
                // Keo tim nguoi choi phai cong khai thi moi hien o Trang chu va trang CLB.
                .visibility(playerCall != null ? PostVisibility.PUBLIC : command.getVisibility())
                .sport(playerCall != null ? playerCall.sport() : command.getSport())
                .playerCall(playerCall)
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .attachments(toAttachments(command.getAttachments()))
                .tags(PostTags.extract(content))
                .mentions(PostMentions.sanitize(command.getMentions(), content, actorId, actor.blockedIds()))
                .build());
        notifyPostMentions(post, List.of());
        return assembler.toResponse(post, actor);
    }

    @Override
    public PostResponseCommand publishHighlight(PublishHighlightCommand command) throws ServiceException {
        UUID authorId = requireActor(command.authorId());
        PostHighlight highlight = validHighlight(command.highlight());
        if (postRepository.existsHighlight(highlight.kind(), highlight.refId(), authorId, highlight.clubId())) {
            throw new ServiceException(highlight.kind() == HighlightKind.MATCH
                    ? "Bạn đã khoe trận này lên Cộng đồng rồi."
                    : "Câu lạc bộ đã khoe giải này lên Cộng đồng rồi.");
        }
        String content = normalizeContent(command.content());
        if (content != null && content.length() > MAX_HIGHLIGHT_CONTENT) {
            throw new ServiceException("Nội dung không được vượt quá " + MAX_HIGHLIGHT_CONTENT + " ký tự");
        }
        Viewer author = assembler.viewer(authorId);
        // Khong gan the chinh minh hay nguoi co quan he chan; bo trung, giu thu tu dich vu goc gui.
        List<UUID> tagged = highlight.tagged().stream().filter(Objects::nonNull).distinct()
                .filter(id -> !id.equals(authorId) && !author.blockedIds().contains(id))
                .limit(PostHighlight.MAX_TAGGED).toList();
        LocalDateTime now = LocalDateTime.now();
        Post post = postRepository.save(Post.builder()
                .authorId(authorId)
                .content(content)
                // Nguoi duoc gan the (vd doi thu chua ket ban) phai xem duoc bai.
                .visibility(PostVisibility.PUBLIC)
                .sport(command.sport())
                .highlight(highlight.withTagged(tagged))
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .tags(PostTags.extract(content))
                .build());
        for (UUID userId : tagged) {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("postId", post.getPostId().toString());
            payload.put("snippet", snippet(highlight.title()));
            payload.put("kind", highlight.kind().name());
            eventPublisher.publish(SocialEventPublisher.POST_TAGGED, userId, authorId, post.getPostId(), payload,
                    "tagged:" + post.getPostId() + ":" + userId);
        }
        return assembler.toResponse(post, author);
    }

    /** Ban chup do dich vu noi bo gui: chi kiem do dai, khong tin la da sach. */
    private static PostHighlight validHighlight(PostHighlight value) throws ServiceException {
        if (value == null || value.kind() == null || value.refId() == null) {
            throw new ServiceException("Thiếu thông tin trận đấu hoặc giải đấu");
        }
        String title = value.title() == null ? "" : value.title().strip();
        if (title.isEmpty() || title.length() > 120) throw new ServiceException("Tiêu đề phải có từ 1 đến 120 ký tự");
        List<PostHighlight.Side> sides = value.sides() == null ? List.of() : value.sides();
        List<PostHighlight.Stat> stats = value.stats() == null ? List.of() : value.stats();
        if (sides.size() > 2 || stats.size() > 6) throw new ServiceException("Thẻ thành tích không hợp lệ");
        if (tooLong(value.headline(), 200) || tooLong(value.subtitle(), 300) || tooLong(value.result(), 120)
                || tooLong(value.clubName(), 150)
                || sides.stream().anyMatch(side -> side == null || tooLong(side.name(), 150))
                || stats.stream().anyMatch(stat -> stat == null || tooLong(stat.label(), 40) || tooLong(stat.value(), 40))) {
            throw new ServiceException("Thẻ thành tích không hợp lệ");
        }
        return new PostHighlight(value.kind(), value.refId(), title, value.headline(), value.subtitle(), value.result(),
                sides, stats, value.clubId(), value.clubName(), value.tagged() == null ? List.of() : value.tagged());
    }

    private static boolean tooLong(String value, int max) {
        return value != null && value.length() > max;
    }

    @Override
    public PostResponseCommand updatePost(UUID postId, SavePostCommand command) throws ServiceException {
        UUID actorId = requireActor(command.getActorId());
        Post post = ownedPost(postId, actorId);
        if (post.getStatus() == PostStatus.REMOVED) {
            throw new ServiceException("Bài viết đã bị xóa");
        }
        Viewer actor = assembler.viewer(actorId);
        List<Mention> previousMentions = post.getMentions();
        if (post.getSharedPostId() != null) {
            // Bai chia se chi sua duoc loi dan va quyen rieng tu; noi dung chinh thuoc bai goc.
            if (command.getVisibility() == null) {
                throw new ServiceException("Quyền riêng tư của bài viết không được để trống");
            }
            post.setContent(caption(command.getContent()));
        } else {
            command.setHighlight(post.getHighlight() != null);
            if (post.getPlayerCall() != null && command.getPlayerCall() == null) {
                // Sua loi nhan cua keo ma client khong gui lai phan keo: giu nguyen keo.
                command.setPlayerCall(post.getPlayerCall());
            }
            command.validate();
            post.setContent(normalizeContent(command.getContent()));
            post.setPlayerCall(playerCall(command.getPlayerCall(), actorId, post.getPlayerCall()));
            // The "khoe" giu nguyen mon cua tran / giai.
            if (post.getHighlight() == null) {
                post.setSport(post.getPlayerCall() != null ? post.getPlayerCall().sport() : command.getSport());
            }
            post.setAttachments(toAttachments(command.getAttachments()));
        }
        post.setVisibility(post.getPlayerCall() != null || post.getHighlight() != null
                ? PostVisibility.PUBLIC : command.getVisibility());
        post.setTags(PostTags.extract(post.getContent()));
        post.setMentions(PostMentions.sanitize(command.getMentions(), post.getContent(), actorId, actor.blockedIds()));
        post.setUpdatedAt(LocalDateTime.now());
        Post saved = postRepository.save(post);
        notifyPostMentions(saved, previousMentions);
        return assembler.toResponse(saved, actor);
    }

    @Override
    public void deletePost(UUID postId, UUID actorId) throws ServiceException {
        Post post = ownedPost(postId, requireActor(actorId));
        post.setStatus(PostStatus.REMOVED);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    public PostResponseCommand sharePost(UUID postId, SharePostCommand command) throws ServiceException {
        Viewer actor = assembler.viewer(requireActor(command.getActorId()));
        Post target = publishedPost(postId);
        ensureVisible(target, actor);
        // Chia se lai mot bai chia se thi tro thang ve bai goc, khong tao chuoi long nhau.
        Post original = target.getSharedPostId() == null ? target : publishedPost(target.getSharedPostId());
        ensureVisible(original, actor);
        if (original.getVisibility() != PostVisibility.PUBLIC) {
            throw new ServiceException("Chỉ có thể chia sẻ bài viết công khai");
        }
        String caption = caption(command.getCaption());
        LocalDateTime now = LocalDateTime.now();
        // Luot chia se duoc dem tu chinh cac bai chia se con hien thi, nen xoa / an bai chia se thi luot giam theo.
        Post share = postRepository.save(Post.builder()
                .authorId(actor.id())
                .content(caption)
                .visibility(command.getVisibility() == null ? PostVisibility.PUBLIC : command.getVisibility())
                .sport(original.getSport())
                .sharedPostId(original.getPostId())
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .tags(PostTags.extract(caption))
                .build());
        recordActivity(ActivityEventPublisher.POST_SHARED, actor.id(), original, Map.of());
        notify(SocialEventPublisher.POST_SHARED, original.getAuthorId(), actor.id(), original, snippet(caption), null);
        return assembler.toResponse(share, actor);
    }

    // ---- reactions -----------------------------------------------------------------------------

    @Override
    public PostResponseCommand likePost(UUID postId, UUID actorId) throws ServiceException {
        Viewer actor = assembler.viewer(requireActor(actorId));
        Post post = publishedPost(postId);
        ensureVisible(post, actor);
        if (addReaction(postId, actor.id(), ReactionType.LIKE)) {
            recordReaction(actor.id(), post, ReactionType.LIKE, true);
            // Cung khoa → cung eventId: bo thich roi thich lai khong bao lan nua.
            notify(SocialEventPublisher.POST_LIKED, post.getAuthorId(), actor.id(), post, snippet(post.getContent()),
                    "like:" + postId + ":" + actor.id());
        }
        return assembler.toResponse(post, actor);
    }

    @Override
    public PostResponseCommand unlikePost(UUID postId, UUID actorId) throws ServiceException {
        Viewer actor = assembler.viewer(requireActor(actorId));
        Post post = publishedPost(postId);
        ensureVisible(post, actor);
        reactionRepository.delete(postId, actor.id(), ReactionType.LIKE);
        recordReaction(actor.id(), post, ReactionType.LIKE, false);
        return assembler.toResponse(post, actor);
    }

    @Override
    public PostResponseCommand savePost(UUID postId, UUID actorId) throws ServiceException {
        Viewer actor = assembler.viewer(requireActor(actorId));
        Post post = publishedPost(postId);
        ensureVisible(post, actor);
        if (addReaction(postId, actor.id(), ReactionType.SAVE)) {
            recordReaction(actor.id(), post, ReactionType.SAVE, true);
        }
        return assembler.toResponse(post, actor);
    }

    @Override
    public PostResponseCommand unsavePost(UUID postId, UUID actorId) throws ServiceException {
        Viewer actor = assembler.viewer(requireActor(actorId));
        // Bo luu luon duoc phep, ke ca khi bai da bi an, de nguoi dung don duoc danh sach cua minh.
        reactionRepository.delete(postId, actor.id(), ReactionType.SAVE);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết"));
        recordReaction(actor.id(), post, ReactionType.SAVE, false);
        return assembler.toResponse(post, actor);
    }

    // ---- comments ------------------------------------------------------------------------------

    @Override
    public Page<PostCommentResponseCommand> getComments(UUID postId, UUID viewerId, Pageable pageable)
            throws ServiceException {
        Post post = publishedPost(postId);
        ensureVisible(post, assembler.viewer(viewerId));
        return commentRepository.findPublishedByPostId(postId, pageable).map(this::toResponse);
    }

    @Override
    public PostCommentResponseCommand createComment(UUID postId, SaveCommentCommand command)
            throws ServiceException {
        Viewer actor = assembler.viewer(requireActor(command.getActorId()));
        Post post = publishedPost(postId);
        ensureVisible(post, actor);
        String content = requiredText(command.getContent(), "Nội dung bình luận không được để trống", 1000);
        PostComment parent = null;
        if (command.getParentCommentId() != null) {
            parent = commentRepository.findById(command.getParentCommentId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy bình luận được trả lời"));
            if (!postId.equals(parent.getPostId()) || parent.getStatus() != PostStatus.PUBLISHED) {
                throw new ServiceException("Bình luận được trả lời không thuộc bài viết hiện tại");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        PostComment saved = commentRepository.save(PostComment.builder()
                .postId(postId)
                .authorId(actor.id())
                .parentCommentId(command.getParentCommentId())
                .content(content)
                .mentions(PostMentions.sanitize(command.getMentions(), content, actor.id(), actor.blockedIds()))
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .build());
        if (saved.getCommentId() != null) {
            recordActivity(ActivityEventPublisher.POST_COMMENT, actor.id(), post,
                    Map.of("commentId", saved.getCommentId().toString(), "active", true));
        }
        String snippet = snippet(content);
        notify(SocialEventPublisher.POST_COMMENTED, post.getAuthorId(), actor.id(), post, snippet, null);
        Set<UUID> alreadyTold = new HashSet<>(Set.of(post.getAuthorId()));
        if (parent != null && !parent.getAuthorId().equals(post.getAuthorId())
                && !actor.blockedIds().contains(parent.getAuthorId())) {
            notify(SocialEventPublisher.COMMENT_REPLIED, parent.getAuthorId(), actor.id(), post, snippet, null);
            alreadyTold.add(parent.getAuthorId());
        }
        notifyCommentMentions(post, saved, List.of(), alreadyTold);
        return toResponse(saved);
    }

    @Override
    public PostCommentResponseCommand updateComment(UUID commentId, UUID actorId, String content, List<Mention> mentions)
            throws ServiceException {
        PostComment comment = ownedComment(commentId, requireActor(actorId));
        if (comment.getStatus() != PostStatus.PUBLISHED) {
            throw new ServiceException("Bình luận không còn khả dụng");
        }
        Viewer actor = assembler.viewer(actorId);
        List<Mention> previous = comment.getMentions();
        String text = requiredText(content, "Nội dung bình luận không được để trống", 1000);
        comment.setContent(text);
        comment.setMentions(PostMentions.sanitize(mentions, text, actorId, actor.blockedIds()));
        comment.setUpdatedAt(LocalDateTime.now());
        PostComment saved = commentRepository.save(comment);
        postRepository.findById(saved.getPostId())
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED)
                .ifPresent(post -> notifyCommentMentions(post, saved, previous, Set.of()));
        return toResponse(saved);
    }

    @Override
    public void deleteComment(UUID commentId, UUID actorId) throws ServiceException {
        PostComment comment = ownedComment(commentId, requireActor(actorId));
        comment.setStatus(PostStatus.REMOVED);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        activityPublisher.publish(ActivityEventPublisher.POST_COMMENT, actorId, Map.of(
                "postId", comment.getPostId().toString(), "commentId", commentId.toString(), "active", false));
    }

    // ---- helpers -------------------------------------------------------------------------------

    /**
     * Keo dang thay CLB: chi chu hoac quan ly CLB (theo club-service) moi dang duoc, va keo phai cung mon voi CLB.
     * Ten CLB lay tu club-service, khong tin ten do client gui. Sua keo giu nguyen trang thai "da du nguoi".
     */
    private PlayerCall playerCall(PlayerCall requested, UUID actorId, PlayerCall previous) throws ServiceException {
        if (requested == null) return null;
        PlayerCall call = requested.withFilled(previous != null && previous.filled());
        if (call.clubId() == null) return call.withClub(null, null);
        if (previous != null && call.clubId().equals(previous.clubId()) && previous.clubName() != null) {
            return call.withClub(previous.clubId(), previous.clubName());
        }
        ClubDirectory.Club club = clubDirectory.find(call.clubId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy câu lạc bộ"));
        if (!club.managerIds().contains(actorId)) {
            throw new ServiceException("Chỉ chủ CLB hoặc quản lý mới được đăng kèo thay CLB");
        }
        if (club.sportType() != null && club.sportType() != call.sport()) {
            throw new ServiceException("Kèo phải cùng môn với câu lạc bộ");
        }
        return call.withClub(club.clubId(), club.name());
    }

    private static boolean isExplore(FeedFilter filter) {
        return !filter.followingOnly() && filter.authorId() == null && filter.tag() == null
                && !filter.playerCallsOnly() && !filter.withFriends() && filter.keyword() == null;
    }

    /**
     * Khám phá cho người đã đăng nhập: lấy {@link #RANKED_WINDOW} bài mới nhất người xem được thấy (cùng bộ lọc môn),
     * nhờ AI chấm điểm rồi sắp lại; phần sau cửa sổ giữ thứ tự mới nhất, nên tổng số bài và cách phân trang không đổi.
     * AI không dùng được thì trả đúng thứ tự mới nhất.
     */
    private Page<PostResponseCommand> rankedExplore(Viewer viewer, FeedFilter filter, Pageable pageable) {
        PostRepository.FeedQuery query = feedQuery(viewer, filter);
        int start = (int) pageable.getOffset();
        if (start >= RANKED_WINDOW) {
            return page(postRepository.findFeed(query, pageable), viewer);
        }
        Page<Post> window = postRepository.findFeed(query, PageRequest.of(0, RANKED_WINDOW, pageable.getSort()));
        List<PostResponseCommand> ranked = new ArrayList<>(assembler.toResponses(window.getContent(), viewer));
        Map<UUID, Double> scores = postRanker.rank(viewer.id(), ranked.stream()
                .map(post -> new PostRanker.Candidate(post.getPostId(), post.getAuthorId(), post.getSport(),
                        post.getLikeCount(), post.getCommentCount(), post.getPublishedAt()))
                .toList());
        if (!scores.isEmpty()) {
            // Sắp xếp ổn định: cùng điểm thì giữ thứ tự mới nhất.
            ranked.sort(Comparator.comparingDouble((PostResponseCommand post) ->
                    scores.getOrDefault(post.getPostId(), 0.0)).reversed());
        }
        int end = start + pageable.getPageSize();
        List<PostResponseCommand> items = new ArrayList<>(ranked.subList(Math.min(start, ranked.size()),
                Math.min(end, ranked.size())));
        if (end > ranked.size() && window.getTotalElements() > ranked.size()) {
            // Trang chạm cuối cửa sổ: phần còn lại lấy theo thứ tự mới nhất, bắt đầu từ bài thứ RANKED_WINDOW.
            List<Post> chronological = postRepository.findFeed(query, pageable).getContent();
            int skip = Math.max(0, ranked.size() - start);
            if (skip < chronological.size()) {
                items.addAll(assembler.toResponses(chronological.subList(skip, chronological.size()), viewer));
            }
        }
        return new PageImpl<>(items, pageable, window.getTotalElements());
    }

    private void recordReaction(UUID actorId, Post post, ReactionType type, boolean active) {
        recordActivity(ActivityEventPublisher.POST_REACTION, actorId, post,
                Map.of("reaction", type.name(), "active", active));
    }

    private void recordActivity(String eventType, UUID actorId, Post post, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>(extra);
        payload.put("postId", post.getPostId().toString());
        payload.put("authorId", post.getAuthorId().toString());
        if (post.getSport() != null) payload.put("sport", post.getSport().name());
        activityPublisher.publish(eventType, actorId, payload);
    }

    private Page<PostResponseCommand> page(Page<Post> posts, Viewer viewer) {
        return new PageImpl<>(assembler.toResponses(posts.getContent(), viewer), posts.getPageable(),
                posts.getTotalElements());
    }

    private PostRepository.FeedQuery feedQuery(Viewer viewer, FeedFilter filter) {
        boolean followingOnly = filter.followingOnly() && viewer.id() != null;
        return new PostRepository.FeedQuery(
                viewer.id(),
                viewer.friendIds(),
                followingOnly ? followedAuthors(viewer, filter.withFriends()) : Set.of(),
                viewer.blockedIds(),
                followingOnly,
                filter.sport(),
                filter.authorId(),
                PostTags.normalize(filter.tag()),
                filter.playerCallsOnly() ? new PostRepository.PlayerCallFilter(null, true) : null,
                filter.sports() == null ? Set.of() : filter.sports(),
                filter.keyword());
    }

    /** Tac gia cua bang tin "Dang theo doi": nguoi minh theo doi (va ban be neu can), khong bao gio chinh minh. */
    private Set<UUID> followedAuthors(Viewer viewer, boolean withFriends) {
        Set<UUID> authors = new HashSet<>(assembler.followingIds(viewer.id()));
        if (withFriends) authors.addAll(viewer.friendIds());
        authors.remove(viewer.id());
        return authors;
    }

    /** Chi bao nguoi moi duoc nhac va xem duoc bai; sua bai khong bao lai nguoi da nhac truoc do. */
    private void notifyPostMentions(Post post, List<Mention> previous) {
        for (UUID userId : PostMentions.added(previous, post.getMentions())) {
            if (!assembler.viewer(userId).canView(post)) continue;
            notify(SocialEventPublisher.POST_MENTIONED, userId, post.getAuthorId(), post, snippet(post.getContent()),
                    "mention:" + post.getPostId() + ":" + userId);
        }
    }

    /** Nhu tren cho binh luan; bo nguoi vua nhan thong bao binh luan / tra loi de khong bao hai lan. */
    private void notifyCommentMentions(Post post, PostComment comment, List<Mention> previous, Set<UUID> alreadyTold) {
        for (UUID userId : PostMentions.added(previous, comment.getMentions())) {
            if (alreadyTold.contains(userId) || !assembler.viewer(userId).canView(post)) continue;
            notify(SocialEventPublisher.COMMENT_MENTIONED, userId, comment.getAuthorId(), post, snippet(comment.getContent()),
                    "comment-mention:" + comment.getCommentId() + ":" + userId);
        }
    }

    private void ensureVisible(Post post, Viewer viewer) throws ServiceException {
        if (viewer.canView(post)) return;
        if (viewer.id() == null) throw new UnauthenticatedException("Bạn cần đăng nhập để xem bài viết này");
        throw new ServiceException("Bạn không có quyền xem bài viết này");
    }

    /** true khi vua tao moi (truoc do chua co). */
    private boolean addReaction(UUID postId, UUID userId, ReactionType type) {
        if (reactionRepository.find(postId, userId, type).isPresent()) return false;
        reactionRepository.save(PostReaction.builder()
                .postId(postId)
                .userId(userId)
                .type(type)
                .reactedAt(LocalDateTime.now())
                .build());
        return true;
    }

    /** Khong bao cho chinh minh. Quan he chan voi tac gia bai da bi canView loai truoc khi toi day. */
    private void notify(String eventType, UUID receiverId, UUID actorId, Post post, String snippet, String dedupeKey) {
        if (receiverId == null || receiverId.equals(actorId)) return;
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("postId", post.getPostId().toString());
        payload.put("snippet", snippet);
        eventPublisher.publish(eventType, receiverId, actorId, post.getPostId(), payload, dedupeKey);
    }

    static String snippet(String text) {
        if (text == null || text.isBlank()) return "";
        String flat = text.strip().replaceAll("\\s+", " ");
        return flat.length() <= 80 ? flat : flat.substring(0, 79) + "…";
    }

    private List<PostAttachment> toAttachments(List<SavePostCommand.AttachmentItem> items) {
        if (items == null) return List.of();
        List<SavePostCommand.AttachmentItem> ordered = items.stream()
                .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? 0 : item.getDisplayOrder()))
                .toList();

        // Anh vua upload nam o `temp/`; doi sang vung vinh vien truoc khi luu khoa vao bai viet,
        // neu khong thi anh chon roi khong dang se tich lai mai va lifecycle rule tren `temp/`
        // se xoa mat anh cua bai da dang.
        List<String> promoted = storageKeyPromoter.promote(
                ordered.stream().map(item -> item.getStorageKey().trim()).toList(),
                "social-post");

        List<PostAttachment> attachments = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            SavePostCommand.AttachmentItem item = ordered.get(index);
            attachments.add(PostAttachment.builder()
                    .storageKey(promoted.get(index))
                    .type(item.getType())
                    .displayOrder(item.getDisplayOrder() == null ? 0 : item.getDisplayOrder())
                    .build());
        }
        return attachments;
    }

    private Post publishedPost(UUID postId) throws ServiceException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết"));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ServiceException("Bài viết không còn khả dụng");
        }
        return post;
    }

    private Post ownedPost(UUID postId, UUID actorId) throws ServiceException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bài viết"));
        if (!actorId.equals(post.getAuthorId())) {
            throw new ServiceException("Bạn không có quyền chỉnh sửa bài viết này");
        }
        return post;
    }

    private PostComment ownedComment(UUID commentId, UUID actorId) throws ServiceException {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy bình luận"));
        if (!actorId.equals(comment.getAuthorId())) {
            throw new ServiceException("Bạn không có quyền chỉnh sửa bình luận này");
        }
        return comment;
    }

    private PostCommentResponseCommand toResponse(PostComment comment) {
        return PostCommentResponseCommand.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .mentions(comment.getMentions())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private UUID requireActor(UUID actorId) throws ServiceException {
        if (actorId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
        return actorId;
    }

    private String caption(String value) throws ServiceException {
        return value == null || value.isBlank() ? null : requiredText(value, "Chú thích không hợp lệ", 1000);
    }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.trim();
    }

    private String requiredText(String value, String emptyMessage, int maxLength) throws ServiceException {
        if (value == null || value.isBlank()) throw new ServiceException(emptyMessage);
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ServiceException("Nội dung không được vượt quá " + maxLength + " ký tự");
        }
        return normalized;
    }
}
