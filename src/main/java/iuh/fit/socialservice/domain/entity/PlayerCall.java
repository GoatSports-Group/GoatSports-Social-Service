package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.PostSport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Bai "Tim nguoi choi": keo dang thieu nguoi. Khong co dang ky hay duyet — nguoi quan tam nhan tin cho nguoi dang
 * roi hai ben tu chot san. {@code clubId} co gia tri khi chu hoac quan ly CLB dang thay CLB.
 *
 * @param slots so nguoi con thieu
 */
public record PlayerCall(
        PostSport sport,
        String playFormat,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        int slots,
        String skillNote,
        UUID clubId,
        String clubName,
        boolean filled
) {
    public static final int MAX_SLOTS = 30;

    public PlayerCall withClub(UUID id, String name) {
        return new PlayerCall(sport, playFormat, playDate, startTime, endTime, location, slots, skillNote, id, name, filled);
    }

    public PlayerCall withFilled(boolean value) {
        return new PlayerCall(sport, playFormat, playDate, startTime, endTime, location, slots, skillNote, clubId, clubName, value);
    }
}
