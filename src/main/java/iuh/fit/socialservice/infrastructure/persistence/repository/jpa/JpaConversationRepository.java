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

    @Query("""
            SELECT DISTINCT other.userId, c.type, c.name
            FROM ConversationMemberEntity me
            JOIN me.conversation c
            JOIN ConversationMemberEntity other ON other.conversation = c
            WHERE me.userId = :userId AND me.leftAt IS NULL
              AND other.userId <> :userId AND other.leftAt IS NULL
              AND c.type IN :types
            """)
    List<Object[]> findCoMembers(@Param("userId") UUID userId, @Param("types") Collection<ConversationType> types);

    @EntityGraph(attributePaths = "members")
    List<ConversationEntity> findByContextIdOrderByCreatedAtAsc(@Param("contextId") UUID contextId);

    /**
     * Mọi cuộc trò chuyện 1-1 (DIRECT hoặc MATCH) giữa đúng hai người này, hoạt động gần nhất trước.
     * Dùng để tái sử dụng luồng chat sẵn có thay vì tạo luồng mới cho mỗi kèo.
     */
    @EntityGraph(attributePaths = "members")
    @Query("""
            SELECT c FROM ConversationEntity c
            WHERE c.type IN :types
              AND (SELECT COUNT(m) FROM ConversationMemberEntity m
                   WHERE m.conversation = c AND m.leftAt IS NULL) = 2
              AND EXISTS (SELECT m1 FROM ConversationMemberEntity m1
                          WHERE m1.conversation = c AND m1.userId = :firstUserId AND m1.leftAt IS NULL)
              AND EXISTS (SELECT m2 FROM ConversationMemberEntity m2
                          WHERE m2.conversation = c AND m2.userId = :secondUserId AND m2.leftAt IS NULL)
            ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC
            """)
    List<ConversationEntity> findPairConversations(
            @Param("types") Collection<ConversationType> types,
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );

    @EntityGraph(attributePaths = "members")
    @Query("""
            SELECT c FROM ConversationEntity c
            WHERE c.type IN :types
              AND (SELECT COUNT(m) FROM ConversationMemberEntity m
                   WHERE m.conversation = c AND m.leftAt IS NULL) = 2
            ORDER BY c.createdAt ASC
            """)
    List<ConversationEntity> findAllPairConversations(@Param("types") Collection<ConversationType> types);
}
