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

    /** Doan chat da xoa phia minh chi hien lai khi co tin moi sau moc xoa. */
    String VISIBLE_AFTER_CLEAR = "AND (m.clearedAt IS NULL OR (c.lastMessageAt IS NOT NULL AND c.lastMessageAt > m.clearedAt))";

    @Override
    @EntityGraph(attributePaths = "members")
    Optional<ConversationEntity> findById(UUID conversationId);

    @Query(
            value = "SELECT c.conversationId FROM ConversationEntity c " +
                    "WHERE EXISTS (SELECT m.memberId FROM ConversationMemberEntity m " +
                    "WHERE m.conversation = c AND m.userId = :userId AND m.leftAt IS NULL " +
                    VISIBLE_AFTER_CLEAR + ") " +
                    "ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC",
            countQuery = "SELECT COUNT(c) FROM ConversationEntity c " +
                    "WHERE EXISTS (SELECT m.memberId FROM ConversationMemberEntity m " +
                    "WHERE m.conversation = c AND m.userId = :userId AND m.leftAt IS NULL " +
                    VISIBLE_AFTER_CLEAR + ")"
    )
    Page<UUID> findUserConversationIds(@Param("userId") UUID userId, Pageable pageable);

    /** Nhu findUserConversationIds nhung chi cac loai trong {@code types} (vd. hop thu cong viec cua chu san). */
    @Query(
            value = "SELECT c.conversationId FROM ConversationEntity c " +
                    "WHERE c.type IN :types AND EXISTS (SELECT m.memberId FROM ConversationMemberEntity m " +
                    "WHERE m.conversation = c AND m.userId = :userId AND m.leftAt IS NULL " +
                    VISIBLE_AFTER_CLEAR + ") " +
                    "ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC",
            countQuery = "SELECT COUNT(c) FROM ConversationEntity c " +
                    "WHERE c.type IN :types AND EXISTS (SELECT m.memberId FROM ConversationMemberEntity m " +
                    "WHERE m.conversation = c AND m.userId = :userId AND m.leftAt IS NULL " +
                    VISIBLE_AFTER_CLEAR + ")"
    )
    Page<UUID> findUserConversationIdsOfTypes(@Param("userId") UUID userId,
                                              @Param("types") Collection<ConversationType> types,
                                              Pageable pageable);

    /** Hop thu ho tro cua doi quan tri: moi doan chat SUPPORT, khong phu thuoc admin da tham gia hay chua. */
    @Query(
            value = "SELECT c.conversationId FROM ConversationEntity c WHERE c.type = :type " +
                    "ORDER BY COALESCE(c.lastMessageAt, c.updatedAt, c.createdAt) DESC",
            countQuery = "SELECT COUNT(c) FROM ConversationEntity c WHERE c.type = :type"
    )
    Page<UUID> findConversationIdsByType(@Param("type") ConversationType type, Pageable pageable);

    /** Doan chat SUPPORT cua mot chu san (chu san la thanh vien vai tro OWNER). */
    @EntityGraph(attributePaths = "members")
    @Query("""
            SELECT c FROM ConversationEntity c
            WHERE c.type = :type
              AND EXISTS (SELECT m FROM ConversationMemberEntity m
                          WHERE m.conversation = c AND m.userId = :ownerId AND m.role = :ownerRole)
            ORDER BY c.createdAt ASC
            """)
    List<ConversationEntity> findOwnedByType(@Param("type") ConversationType type,
                                             @Param("ownerId") UUID ownerId,
                                             @Param("ownerRole") iuh.fit.socialservice.domain.enums.MemberRole ownerRole);

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

    /** Rời mọi nhóm chat (giữ chat 1-1 để người kia còn lịch sử). */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
            update ConversationMemberEntity m set m.leftAt = :now
            where m.userId = :userId and m.leftAt is null
              and m.conversation.conversationId in (
                  select c.conversationId from ConversationEntity c
                  where c.type <> iuh.fit.socialservice.domain.enums.ConversationType.DIRECT)
            """)
    int leaveGroupConversations(@org.springframework.data.repository.query.Param("userId") UUID userId,
                                @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
