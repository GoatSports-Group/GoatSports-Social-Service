package iuh.fit.socialservice.infrastructure.persistence.repository.jpa;

import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "members")
    Optional<ConversationEntity> findById(UUID conversationId);

    @Query(
            value = "SELECT c.conversationId FROM ConversationEntity c " +
                    "WHERE EXISTS (SELECT m.memberId FROM ConversationMemberEntity m " +
                    "WHERE m.conversation = c AND m.userId = :userId AND m.leftAt IS NULL) " +
                    "ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC",
            countQuery = "SELECT COUNT(c) FROM ConversationEntity c " +
                    "WHERE EXISTS (SELECT m.memberId FROM ConversationMemberEntity m " +
                    "WHERE m.conversation = c AND m.userId = :userId AND m.leftAt IS NULL)"
    )
    Page<UUID> findUserConversationIds(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "members")
    List<ConversationEntity> findAllByConversationIdIn(Collection<UUID> conversationIds);

    @EntityGraph(attributePaths = "members")
    @Query("""
            SELECT c FROM ConversationEntity c
            WHERE c.type = :type
              AND (SELECT COUNT(m) FROM ConversationMemberEntity m
                   WHERE m.conversation = c AND m.leftAt IS NULL) = 2
              AND EXISTS (SELECT m1 FROM ConversationMemberEntity m1
                          WHERE m1.conversation = c AND m1.userId = :firstUserId AND m1.leftAt IS NULL)
              AND EXISTS (SELECT m2 FROM ConversationMemberEntity m2
                          WHERE m2.conversation = c AND m2.userId = :secondUserId AND m2.leftAt IS NULL)
            """)
    Optional<ConversationEntity> findDirectBetween(
            @Param("type") ConversationType type,
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );
}
