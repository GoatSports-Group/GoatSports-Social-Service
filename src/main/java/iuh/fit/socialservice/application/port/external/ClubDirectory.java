package iuh.fit.socialservice.application.port.external;

import iuh.fit.socialservice.domain.enums.PostSport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tra CLB ben club-service: ai la chu/quan ly, de chi ho moi dang bai tim nguoi choi thay CLB. */
public interface ClubDirectory {
    /** Rong neu CLB khong ton tai hoac da giai tan. */
    Optional<Club> find(UUID clubId);

    record Club(UUID clubId, String name, PostSport sportType, List<UUID> managerIds) {
    }
}
