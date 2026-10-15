package iuh.fit.socialservice.infrastructure.external.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Timeout riêng ngắn (application.properties, contextId này): bảng tin không được chờ AI. */
@FeignClient(
        name = "GOATSPORTS-AI-SERVICE",
        contextId = "socialAiPersonalizationClient",
        url = "${ai.service.url}",
        path = "/api/v1/ai/personalization",
        configuration = StorageClientConfig.class
)
public interface AiPersonalizationClient {

    @PostMapping("/posts/rank")
    List<PostScore> rankPosts(@RequestBody RankRequest request);

    record RankRequest(UUID userId, List<Candidate> candidates) { }

    record Candidate(UUID postId, UUID authorId, String sport, long likeCount, long commentCount,
                     OffsetDateTime publishedAt) { }

    record PostScore(UUID postId, Double score) { }
}
