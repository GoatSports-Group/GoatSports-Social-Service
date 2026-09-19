package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.infrastructure.persistence.entity.ModerationActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaModerationActionRepository extends JpaRepository<ModerationActionEntity, UUID> {
    List<ModerationActionEntity> findByReport_ReportIdOrderByActionAtAsc(UUID reportId);
}
