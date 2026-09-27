package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.request.SaveCommentCommand;
import iuh.fit.socialservice.application.dto.request.SavePostCommand;
import iuh.fit.socialservice.application.dto.request.SharePostCommand;
import iuh.fit.socialservice.application.dto.response.PostCommentResponseCommand;
import iuh.fit.socialservice.application.dto.response.PostResponseCommand;
import iuh.fit.socialservice.presentation.request.SaveCommentRequest;
import iuh.fit.socialservice.presentation.request.SavePostRequest;
import iuh.fit.socialservice.presentation.request.SharePostRequest;
import iuh.fit.socialservice.presentation.response.PostCommentResponse;
import iuh.fit.socialservice.presentation.response.PostResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SocialPostConverter {
    public SavePostCommand toCommand(UUID actorId, SavePostRequest request) {
        return SavePostCommand.builder()
                .actorId(actorId)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .sport(request.getSport())
                .mentions(request.getMentions())
                .attachments(request.getAttachments() == null ? List.of()
                        : request.getAttachments().stream().map(item -> SavePostCommand.AttachmentItem.builder()
                                .storageKey(item.getStorageKey())
                                .type(item.getType())
                                .displayOrder(item.getDisplayOrder())
                                .build()).toList())
                .build();
    }

    public SaveCommentCommand toCommand(UUID actorId, SaveCommentRequest request) {
        return SaveCommentCommand.builder()
                .actorId(actorId)
                .parentCommentId(request.getParentCommentId())
                .content(request.getContent())
                .build();
    }

    public SharePostCommand toCommand(UUID actorId, SharePostRequest request) {
        return SharePostCommand.builder()
                .actorId(actorId)
                .caption(request.getCaption())
                .visibility(request.getVisibility())
                .build();
    }

    public PostResponse toResponse(PostResponseCommand command) {
        if (command == null) return null;
        return PostResponse.builder()
                .postId(command.getPostId())
                .authorId(command.getAuthorId())
                .content(command.getContent())
                .visibility(command.getVisibility())
                .status(command.getStatus())
                .sport(command.getSport())
                .sharedPostId(command.getSharedPostId())
                .sharedPost(toResponse(command.getSharedPost()))
                .sharedPostUnavailable(command.isSharedPostUnavailable())
                .tags(command.getTags())
                .mentions(command.getMentions())
                .createdAt(command.getCreatedAt())
                .updatedAt(command.getUpdatedAt())
                .publishedAt(command.getPublishedAt())
                .attachments(command.getAttachments().stream().map(item -> PostResponse.AttachmentItem.builder()
                        .attachmentId(item.getAttachmentId())
                        .storageKey(item.getStorageKey())
                        .type(item.getType())
                        .displayOrder(item.getDisplayOrder())
                        .build()).toList())
                .likeCount(command.getLikeCount())
                .commentCount(command.getCommentCount())
                .shareCount(command.getShareCount())
                .likedByCurrentUser(command.isLikedByCurrentUser())
                .savedByCurrentUser(command.isSavedByCurrentUser())
                .build();
    }

    public PostCommentResponse toResponse(PostCommentResponseCommand command) {
        return PostCommentResponse.builder()
                .commentId(command.getCommentId())
                .postId(command.getPostId())
                .authorId(command.getAuthorId())
                .parentCommentId(command.getParentCommentId())
                .content(command.getContent())
                .status(command.getStatus())
                .createdAt(command.getCreatedAt())
                .updatedAt(command.getUpdatedAt())
                .build();
    }
}
