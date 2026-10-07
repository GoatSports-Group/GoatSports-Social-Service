package iuh.fit.socialservice.infrastructure.persistence.repository;

import java.util.Collection;
import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.ConversationEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {
    private static final List<ConversationType> PAIR_CONVERSATION_TYPES =
            List.of(ConversationType.DIRECT, ConversationType.MATCH);

    private final JpaConversationRepository jpaConversationRepository;
    private final ConversationEntityMapper conversationEntityMapper;

    @Override
    @Transactional
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = conversationEntityMapper.toEntity(conversation);
        return conversationEntityMapper.toDomain(jpaConversationRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(UUID conversationId) {
        return jpaConversationRepository.findById(conversationId).map(conversationEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findUserConversations(UUID userId, Pageable pageable) {
        return loadPage(jpaConversationRepository.findUserConversationIds(userId, pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findUserConversations(UUID userId, Collection<ConversationType> types, Pageable pageable) {
        if (types == null || types.isEmpty()) return findUserConversations(userId, pageable);
        return loadPage(jpaConversationRepository.findUserConversationIdsOfTypes(userId, types, pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findByType(ConversationType type, Pageable pageable) {
        return loadPage(jpaConversationRepository.findConversationIdsByType(type, pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findPairOfType(ConversationType type, UUID firstUserId, UUID secondUserId) {
        return jpaConversationRepository.findDirectBetween(type, firstUserId, secondUserId)
                .map(conversationEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findSupportOf(UUID ownerId) {
        return jpaConversationRepository.findOwnedByType(ConversationType.SUPPORT, ownerId,
                        iuh.fit.socialservice.domain.enums.MemberRole.OWNER).stream()
                .findFirst()
                .map(conversationEntityMapper::toDomain);
    }

    /** Trang id da sap xep -> nap ca doan chat kem thanh vien, giu nguyen thu tu. */
    private Page<Conversation> loadPage(Page<UUID> conversationIds, Pageable pageable) {
        if (conversationIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, conversationIds.getTotalElements());
        }

        Map<UUID, ConversationEntity> conversationsById = jpaConversationRepository
                .findAllByConversationIdIn(conversationIds.getContent())
                .stream()
                .collect(Collectors.toMap(
                        ConversationEntity::getConversationId,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<Conversation> conversations = conversationIds.getContent().stream()
                .map(conversationsById::get)
                .filter(Objects::nonNull)
                .map(conversationEntityMapper::toDomain)
                .toList();

        return new PageImpl<>(conversations, pageable, conversationIds.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findDirectBetween(UUID firstUserId, UUID secondUserId) {
        return jpaConversationRepository.findDirectBetween(ConversationType.DIRECT, firstUserId, secondUserId)
                .map(conversationEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findByContextId(UUID contextId) {
        if (contextId == null) return Optional.empty();
        return jpaConversationRepository.findByContextIdOrderByCreatedAtAsc(contextId).stream()
                .findFirst()
                .map(conversationEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findPairConversation(UUID firstUserId, UUID secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            return Optional.empty();
        }
        return jpaConversationRepository
                .findPairConversations(PAIR_CONVERSATION_TYPES, firstUserId, secondUserId)
                .stream()
                .findFirst()
                .map(conversationEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoMember> findCoMembers(UUID userId, Collection<ConversationType> types) {
        if (types.isEmpty()) return List.of();
        return jpaConversationRepository.findCoMembers(userId, types).stream()
                .map(row -> new CoMember((UUID) row[0], (ConversationType) row[1], (String) row[2]))
                .toList();
    }

    @Override
    public List<Conversation> findAllPairConversations() {
        return jpaConversationRepository.findAllPairConversations(PAIR_CONVERSATION_TYPES).stream()
                .map(conversationEntityMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID conversationId) {
        jpaConversationRepository.deleteById(conversationId);
    }
}
