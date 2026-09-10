package iuh.fit.socialservice.presentation.controller;

import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.security.CurrentUserProvider;
import iuh.fit.socialservice.application.service.SocialPostService;
import iuh.fit.socialservice.presentation.annotation.ApiMessage;
import iuh.fit.socialservice.presentation.converter.SocialPostConverter;
import iuh.fit.socialservice.presentation.request.SaveCommentRequest;
import iuh.fit.socialservice.presentation.request.SavePostRequest;
import iuh.fit.socialservice.presentation.request.SharePostRequest;
import iuh.fit.socialservice.presentation.request.UpdateCommentRequest;
import iuh.fit.socialservice.presentation.response.PostCommentResponse;
import iuh.fit.socialservice.presentation.response.PostResponse;
import iuh.fit.socialservice.presentation.response.PostShareResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Page<PostResponse>> getFeed(
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<PostResponse> response = postService.getFeed(currentUserIdOrNull(), pageable)
                .map(postConverter::toResponse);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<Page<PostCommentResponse>> getComments(
            @PathVariable UUID postId,
            @PageableDefault(size = 20) Pageable pageable
    ) throws ServiceException {
        Page<PostCommentResponse> response = postService
                .getComments(postId, currentUserIdOrNull(), pageable)
                .map(postConverter::toResponse);
        return ResponseEntity.ok(response);
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
                postService.updateComment(commentId, requiredUserId(), request.getContent())
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

    @PostMapping("/{postId}/shares")
    @ApiMessage("Chia sẻ bài viết thành công")
    public ResponseEntity<PostShareResponse> sharePost(
            @PathVariable UUID postId,
            @Valid @RequestBody SharePostRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED).body(postConverter.toResponse(
                postService.sharePost(postId, postConverter.toCommand(requiredUserId(), request))
        ));
    }

    private UUID requiredUserId() throws ServiceException {
        UUID userId = currentUserIdOrNull();
        if (userId == null) throw new ServiceException("Phiên đăng nhập đã hết hạn");
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
