package iuh.fit.socialservice.infrastructure.persistence.repository;

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
        Page<UUID> conversationIds = jpaConversationRepository.findUserConversationIds(userId, pageable);
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
    public void deleteById(UUID conversationId) {
        jpaConversationRepository.deleteById(conversationId);
    }
}
