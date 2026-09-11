package iuh.fit.socialservice.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueFollowStatusResponse {
    private UUID venueId;
    private boolean followed;
}
