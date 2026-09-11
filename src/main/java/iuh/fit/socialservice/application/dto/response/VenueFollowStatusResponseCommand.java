package iuh.fit.socialservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueFollowStatusResponseCommand {
    private UUID venueId;
    private boolean followed;
}
