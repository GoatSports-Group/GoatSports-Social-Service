package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.ContentAppeal;
import iuh.fit.socialservice.domain.enums.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ContentAppealRepository {
    ContentAppeal save(ContentAppeal appeal);

    Optional<ContentAppeal> findById(UUID appealId);

    boolean existsByActionId(UUID actionId);

    Page<ContentAppeal> findByStatusIn(Collection<AppealStatus> statuses, Pageable pageable);

    Page<ContentAppeal> findByAppellantId(UUID appellantId, Pageable pageable);
}
