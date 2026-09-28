package iuh.fit.socialservice.infrastructure.external.client;

import iuh.fit.socialservice.application.dto.response.base.BaseResponse;
import iuh.fit.socialservice.domain.enums.PostSport;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-CLUB-SERVICE",
        contextId = "socialClubClient",
        url = "${club.service.url}",
        path = "/internal/clubs",
        configuration = StorageClientConfig.class
)
public interface ClubClient {

    @GetMapping("/{clubId}/matchmaking-profile")
    BaseResponse<ClubProfile> profile(@PathVariable UUID clubId);

    record ClubProfile(UUID clubId, String name, PostSport sportType, List<UUID> managerIds) { }
}
