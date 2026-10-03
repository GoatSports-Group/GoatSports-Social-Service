package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.HighlightKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostHighlightDoc {
    private HighlightKind kind;
    private String refId;
    private String title;
    private String headline;
    private String subtitle;
    private String result;
    @Builder.Default
    private List<SideDoc> sides = new ArrayList<>();
    @Builder.Default
    private List<StatDoc> stats = new ArrayList<>();
    private String clubId;
    private String clubName;
    @Builder.Default
    private List<String> tagged = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SideDoc {
        private String name;
        private Integer score;
        private boolean winner;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatDoc {
        private String label;
        private String value;
    }
}
