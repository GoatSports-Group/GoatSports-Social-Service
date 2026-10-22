package iuh.fit.socialservice.infrastructure.external.adapter;

import feign.FeignException;
import iuh.fit.socialservice.application.dto.response.base.BaseResponse;
import iuh.fit.socialservice.application.port.external.ClubDirectory;
import iuh.fit.socialservice.infrastructure.external.client.ClubClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClubDirectoryAdapter implements ClubDirectory {
    private final ClubClient clubClient;

    @Override
    public Optional<Club> find(UUID clubId) {
        try {
            BaseResponse<ClubClient.ClubProfile> response = clubClient.profile(clubId);
            ClubClient.ClubProfile club = response == null ? null : response.getData();
            if (club == null) return Optional.empty();
            return Optional.of(new Club(club.clubId(), club.name(), club.sportType(),
                    club.managerIds() == null ? List.of() : club.managerIds()));
        } catch (FeignException.NotFound exception) {
            return Optional.empty();
        }
    }
}
