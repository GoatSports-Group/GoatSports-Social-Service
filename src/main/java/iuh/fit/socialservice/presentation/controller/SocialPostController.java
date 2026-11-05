package iuh.fit.socialservice.presentation.controller;

import java.util.Set;
import iuh.fit.socialservice.application.exception.UnauthenticatedException;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.PostRepository;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialPostService;
import iuh.fit.socialservice.domain.enums.PostSport;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialPostConverter;
import iuh.fit.socialservice.presentation.request.SaveCommentRequest;
import iuh.fit.socialservice.presentation.request.SavePostRequest;
import iuh.fit.socialservice.presentation.request.SharePostRequest;
import iuh.fit.socialservice.presentation.request.UpdateCommentRequest;
import iuh.fit.socialservice.presentation.response.PostCommentResponse;
import iuh.fit.socialservice.presentation.response.PostResponse;
import iuh.fit.socialservice.presentation.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/posts")
@RequiredArgsConstructor
public class SocialPostController {
    private final SocialPostService postService;
    private final SocialPostConverter postConverter;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @ApiMessage("Lấy bảng tin thành công")
    public ResponseEntity<PageResponse<PostResponse>> getFeed(
            @RequestParam(defaultValue = "false") boolean followingOnly,
            @RequestParam(required = false) PostSport sport,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "false") boolean playerCallsOnly,
            @RequestParam(required = false) Set<PostSport> sports,
            @RequestParam(defaultValue = "false") boolean withFriends,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        SocialPostService.FeedFilter filter = new SocialPostService.FeedFilter(followingOnly, sport, authorId, tag,
                playerCallsOnly, sports == null ? Set.of() : sports, withFriends, keyword(q));
        Page<PostResponse> response = postService.getFeed(currentUserIdOrNull(), filter, pageable)
                .map(postConverter::toResponse);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    private static String keyword(String q) {
        if (q == null || q.isBlank()) return null;
        String value = q.trim().replaceAll("\\s+", " ");
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    @GetMapping("/saved")
    @ApiMessage("Lấy bài viết đã lưu thành công")
    public ResponseEntity<PageResponse<PostResponse>> getSavedPosts(
            @PageableDefault(size = 10) Pageable pageable
    ) throws ServiceException {
        Page<PostResponse> response = postService.getSavedPosts(requiredUserId(), pageable)
                .map(postConverter::toResponse);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/tags/trending")
    @ApiMessage("Lấy chủ đề nổi bật thành công")
    public ResponseEntity<List<PostRepository.TagCount>> getTrendingTags(
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(required = false) PostSport sport,
            @RequestParam(required = false) Set<PostSport> sports
    ) {
        Set<PostSport> filter = sport != null ? Set.of(sport) : sports == null ? Set.of() : sports;
        return ResponseEntity.ok(postService.getTrendingTags(limit, filter));
    }

    /** Chu de hay dung 30 ngay qua, de goi y khi go "#" (client loc theo chu dang go). */
    @GetMapping("/tags/suggestions")
    @ApiMessage("Lấy gợi ý chủ đề thành công")
    public ResponseEntity<List<PostRepository.TagCount>> suggestTags(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(postService.suggestTags(limit));
    }

    @GetMapping("/authors/{authorId}/stats")
    @ApiMessage("Lấy thống kê tác giả thành công")
    public ResponseEntity<SocialPostService.AuthorStats> getAuthorStats(@PathVariable UUID authorId) {
        return ResponseEntity.ok(postService.getAuthorStats(authorId, currentUserIdOrNull()));
    }

    /** Kèo "Tìm người chơi" còn mở: cá nhân cho Trang chủ, CLB cho trang Câu lạc bộ. */
    @GetMapping("/player-calls")
    @ApiMessage("Lấy kèo đang tìm người thành công")
    public ResponseEntity<List<PostResponse>> getOpenPlayerCalls(
            @RequestParam(defaultValue = "false") boolean club,
            @RequestParam(required = false) PostSport sport,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(postService.getOpenPlayerCalls(currentUserIdOrNull(), club, sport, limit).stream()
                .map(postConverter::toResponse).toList());
    }

    @PutMapping("/{postId}/player-call")
    @ApiMessage("Cập nhật kèo thành công")
    public ResponseEntity<PostResponse> setPlayerCallFilled(
            @PathVariable UUID postId,
            @RequestBody PlayerCallStatusRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(
                postService.setPlayerCallFilled(postId, requiredUserId(), request.filled())));
    }

    public record PlayerCallStatusRequest(boolean filled) { }

    @GetMapping("/{postId}")
    @ApiMessage("Lấy bài viết thành công")
    public ResponseEntity<PostResponse> getPost(@PathVariable UUID postId) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(
                postService.getPost(postId, currentUserIdOrNull())
        ));
    }

    @PostMapping
    @ApiMessage("Đăng bài viết thành công")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody SavePostRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED).body(postConverter.toResponse(
                postService.createPost(postConverter.toCommand(requiredUserId(), request))
        ));
    }

    @PutMapping("/{postId}")
    @ApiMessage("Cập nhật bài viết thành công")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody SavePostRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(
                postService.updatePost(postId, postConverter.toCommand(requiredUserId(), request))
        ));
    }

    @DeleteMapping("/{postId}")
    @ApiMessage("Xóa bài viết thành công")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) throws ServiceException {
        postService.deletePost(postId, requiredUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}/comments")
    @ApiMessage("Lấy bình luận thành công")
    public ResponseEntity<PageResponse<PostCommentResponse>> getComments(
            @PathVariable UUID postId,
            @PageableDefault(size = 20) Pageable pageable
    ) throws ServiceException {
        Page<PostCommentResponse> response = postService
                .getComments(postId, currentUserIdOrNull(), pageable)
                .map(postConverter::toResponse);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PostMapping("/{postId}/comments")
    @ApiMessage("Bình luận bài viết thành công")
    public ResponseEntity<PostCommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody SaveCommentRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED).body(postConverter.toResponse(
                postService.createComment(postId, postConverter.toCommand(requiredUserId(), request))
        ));
    }

    @PutMapping("/comments/{commentId}")
    @ApiMessage("Cập nhật bình luận thành công")
    public ResponseEntity<PostCommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(
                postService.updateComment(commentId, requiredUserId(), request.getContent(),
                        postConverter.toMentions(request.getMentions()))
        ));
    }

    @DeleteMapping("/comments/{commentId}")
    @ApiMessage("Xóa bình luận thành công")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) throws ServiceException {
        postService.deleteComment(commentId, requiredUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    @ApiMessage("Thích bài viết thành công")
    public ResponseEntity<PostResponse> likePost(@PathVariable UUID postId) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(postService.likePost(postId, requiredUserId())));
    }

    @DeleteMapping("/{postId}/like")
    @ApiMessage("Bỏ thích bài viết thành công")
    public ResponseEntity<PostResponse> unlikePost(@PathVariable UUID postId) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(postService.unlikePost(postId, requiredUserId())));
    }

    @PutMapping("/{postId}/save")
    @ApiMessage("Lưu bài viết thành công")
    public ResponseEntity<PostResponse> savePost(@PathVariable UUID postId) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(postService.savePost(postId, requiredUserId())));
    }

    @DeleteMapping("/{postId}/save")
    @ApiMessage("Bỏ lưu bài viết thành công")
    public ResponseEntity<PostResponse> unsavePost(@PathVariable UUID postId) throws ServiceException {
        return ResponseEntity.ok(postConverter.toResponse(postService.unsavePost(postId, requiredUserId())));
    }

    @PostMapping("/{postId}/shares")
    @ApiMessage("Chia sẻ bài viết thành công")
    public ResponseEntity<PostResponse> sharePost(
            @PathVariable UUID postId,
            @Valid @RequestBody SharePostRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED).body(postConverter.toResponse(
                postService.sharePost(postId, postConverter.toCommand(requiredUserId(), request))
        ));
    }

    private UUID requiredUserId() throws ServiceException {
        UUID userId = currentUserIdOrNull();
        if (userId == null) throw new UnauthenticatedException("Phiên đăng nhập đã hết hạn");
        return userId;
    }

    private UUID currentUserIdOrNull() {
        return currentUserProvider.getCurrentUserId()
                .filter(this::isUuid)
                .map(UUID::fromString)
                .orElse(null);
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
