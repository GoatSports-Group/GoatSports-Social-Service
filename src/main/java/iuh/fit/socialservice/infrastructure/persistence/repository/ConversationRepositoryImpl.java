package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.application.port.persistence.ConversationRepository;
import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationEntity;
import iuh.fit.socialservice.infrastructure.persistence.mapper.ConversationEntityMapper;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
        return jpaConversationRepository.findUserConversations(userId, pageable)
                .map(conversationEntityMapper::toDomain);
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
