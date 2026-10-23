package iuh.fit.socialservice.infrastructure.persistence.entity;

import iuh.fit.socialservice.domain.enums.PostSport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCallDoc {
    private PostSport sport;
    private String playFormat;
    private LocalDate playDate;
    private String startTime;
    private String endTime;
    private String location;
    private int slots;
    private String skillNote;
    private String clubId;
    private String clubName;
    private boolean filled;
    private String venueKind;
    private String venueId;
    private String venueName;
    private Double venueLatitude;
    private Double venueLongitude;
}
