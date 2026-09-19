package iuh.fit.socialservice.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowStatusResponse {
    private UUID userId;
    private boolean followed;
    private long followerCount;
    private long followingCount;
}
