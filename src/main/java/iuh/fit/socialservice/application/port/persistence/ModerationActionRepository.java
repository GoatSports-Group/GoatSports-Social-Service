package iuh.fit.socialservice.application.port.persistence;

import iuh.fit.socialservice.domain.entity.ModerationAction;
import iuh.fit.socialservice.domain.enums.ModerationActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModerationActionRepository {
    ModerationAction save(ModerationAction action);

    Optional<ModerationAction> findById(UUID actionId);

    List<ModerationAction> findByReportId(UUID reportId);

    /** Quyết định ảnh hưởng tới nội dung / tài khoản của một người, mới nhất trước. */
    Page<ModerationAction> findByContentAuthorId(UUID authorId, Collection<ModerationActionType> types,
                                                 Pageable pageable);
}
