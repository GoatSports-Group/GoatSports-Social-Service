package iuh.fit.socialservice.domain.entity;

import java.util.UUID;

public record PlayerCallVenue(Kind kind, UUID venueId, String name, double latitude, double longitude) {
    public static final int MAX_NAME_LENGTH = 200;

    public enum Kind { PARTNER, EXTERNAL }
}
