package iuh.fit.socialservice.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FollowSuggestionRankerTest {
    @Test
    void sameKindCountsOnceAndClubNamesAreNotDoubled() {
        UUID clubmate = UUID.randomUUID();
        FollowSuggestionRanker ranker = new FollowSuggestionRanker(Set.of());

        ranker.sameClub(clubmate, "CLB Cầu Lông Phú Nhuận");
        ranker.sameClub(clubmate, "Weekend Club");

        FollowSuggestionRanker.Suggestion suggestion = ranker.top(5).get(0);
        assertThat(suggestion.reason()).isEqualTo("Cùng CLB Cầu Lông Phú Nhuận");
        assertThat(suggestion.score()).isEqualTo(40);
    }
}
