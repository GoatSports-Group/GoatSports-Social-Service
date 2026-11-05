package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.AppealStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.ContentAppealEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaContentAppealRepository extends JpaRepository<ContentAppealEntity, UUID> {
    boolean existsByAction_ActionId(UUID actionId);

    Optional<ContentAppealEntity> findByAction_ActionId(UUID actionId);

    Page<ContentAppealEntity> findByStatusInOrderByCreatedAtAsc(Collection<AppealStatus> statuses, Pageable pageable);

    Page<ContentAppealEntity> findByAppellantIdOrderByCreatedAtDesc(UUID appellantId, Pageable pageable);
}
