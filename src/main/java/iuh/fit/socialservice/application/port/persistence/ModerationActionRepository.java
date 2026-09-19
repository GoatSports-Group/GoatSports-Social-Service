package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.ModerationAction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModerationActionRepository {
    ModerationAction save(ModerationAction action);

    Optional<ModerationAction> findById(UUID actionId);

    List<ModerationAction> findByReportId(UUID reportId);
}
