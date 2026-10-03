package iuh.fit.socialservice.presentation.request;

import iuh.fit.socialservice.domain.enums.HighlightKind;
import iuh.fit.socialservice.domain.enums.PostSport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PublishHighlightRequest(
        @NotNull UUID authorId,
        PostSport sport,
        @Size(max = 2000) String content,
        @NotNull HighlightKind kind,
        @NotNull UUID refId,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 200) String headline,
        @Size(max = 300) String subtitle,
        @Size(max = 120) String result,
        @Valid @Size(max = 2) List<Side> sides,
        @Valid @Size(max = 6) List<Stat> stats,
        UUID clubId,
        @Size(max = 150) String clubName,
        @Size(max = 200) List<UUID> tagged
) {
    public record Side(@NotBlank @Size(max = 150) String name, Integer score, boolean winner) {
    }

    public record Stat(@NotBlank @Size(max = 40) String label, @NotBlank @Size(max = 40) String value) {
    }
}
