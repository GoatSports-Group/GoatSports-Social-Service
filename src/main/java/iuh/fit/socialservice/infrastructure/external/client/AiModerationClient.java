package iuh.fit.socialservice.infrastructure.external.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "GOATSPORTS-AI-SERVICE",
        contextId = "socialAiModerationClient",
        url = "${ai.service.url}",
        path = "/api/v1/ai/moderation",
        configuration = StorageClientConfig.class
)
public interface AiModerationClient {

    @PostMapping("/classify")
    Verdict classify(@RequestBody ClassifyRequest request);

    record ClassifyRequest(String kind, String text, String reportReason) { }

    record Verdict(String label, Integer severity, String reason) { }
}
