package iuh.fit.socialservice.presentation.controller.internal;

import iuh.fit.socialservice.application.dto.request.PublishHighlightCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.service.SocialPostService;
import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.presentation.converter.SocialPostConverter;
import iuh.fit.socialservice.presentation.request.PublishHighlightRequest;
import iuh.fit.socialservice.presentation.response.PostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Bai "khoe" do dich vu so huu du lieu dang thay nguoi dung (gateway secret, khong qua client). */
@RestController
@RequestMapping("/internal/posts")
@RequiredArgsConstructor
public class SocialPostInternalController {
    private final SocialPostService postService;
    private final SocialPostConverter postConverter;

    @PostMapping("/highlights")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse publishHighlight(@Valid @RequestBody PublishHighlightRequest request) throws ServiceException {
        PostHighlight highlight = new PostHighlight(request.kind(), request.refId(), request.title(), request.headline(),
                request.subtitle(), request.result(),
                request.sides() == null ? List.of() : request.sides().stream()
                        .map(side -> new PostHighlight.Side(side.name(), side.score(), side.winner())).toList(),
                request.stats() == null ? List.of() : request.stats().stream()
                        .map(stat -> new PostHighlight.Stat(stat.label(), stat.value())).toList(),
                request.clubId(), request.clubName(), request.tagged() == null ? List.of() : request.tagged());
        return postConverter.toResponse(postService.publishHighlight(
                new PublishHighlightCommand(request.authorId(), request.sport(), request.content(), highlight)));
    }
}
