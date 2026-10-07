package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.entity.Conversation;
import iuh.fit.socialservice.domain.entity.ConversationMember;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationMemberEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ConversationEntityMapper {

    public Conversation toDomain(ConversationEntity entity) {
        if (entity == null) return null;
        return Conversation.builder()
                .conversationId(entity.getConversationId())
                .type(entity.getType())
                .name(entity.getName())
                .avatarUrl(entity.getAvatarUrl())
                .lastMessageAt(entity.getLastMessageAt())
                .lastMessageId(entity.getLastMessageId())
                .contextId(entity.getContextId())
                .subjectType(entity.getSubjectType())
                .subjectId(entity.getSubjectId())
                .members(toDomainMembers(entity.getMembers()))
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public ConversationEntity toEntity(Conversation domain) {
        if (domain == null) return null;
        ConversationEntity entity = ConversationEntity.builder()
                .conversationId(domain.getConversationId())
                .type(domain.getType())
                .name(domain.getName())
                .avatarUrl(domain.getAvatarUrl())
                .lastMessageAt(domain.getLastMessageAt())
                .lastMessageId(domain.getLastMessageId())
                .contextId(domain.getContextId())
                .subjectType(domain.getSubjectType())
                .subjectId(domain.getSubjectId())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedAt(domain.getUpdatedAt())
                .updatedBy(domain.getUpdatedBy())
                .build();
        entity.replaceMembers(toEntityMembers(domain.getMembers()));
        return entity;
    }

    private List<ConversationMember> toDomainMembers(List<ConversationMemberEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(this::toDomainMember).toList();
    }

    private List<ConversationMemberEntity> toEntityMembers(List<ConversationMember> members) {
        if (members == null) return Collections.emptyList();
        return members.stream().map(this::toEntityMember).toList();
    }

    private ConversationMember toDomainMember(ConversationMemberEntity entity) {
        ConversationMember member = new ConversationMember();
        member.setMemberId(entity.getMemberId());
        member.setConversationId(entity.getConversation().getConversationId());
        member.setUserId(entity.getUserId());
        member.setRole(entity.getRole());
        member.setJoinedAt(entity.getJoinedAt());
        member.setLastReadMessageId(entity.getLastReadMessageId());
        member.setLeftAt(entity.getLeftAt());
        member.setClearedAt(entity.getClearedAt());
        member.setMuted(entity.isMuted());
        member.setCreatedAt(entity.getCreatedAt());
        member.setCreatedBy(entity.getCreatedBy());
        member.setUpdatedAt(entity.getUpdatedAt());
        member.setUpdatedBy(entity.getUpdatedBy());
        return member;
    }

    private ConversationMemberEntity toEntityMember(ConversationMember member) {
        ConversationMemberEntity entity = new ConversationMemberEntity();
        entity.setMemberId(member.getMemberId());
        entity.setUserId(member.getUserId());
        entity.setRole(member.getRole());
        entity.setJoinedAt(member.getJoinedAt());
        entity.setLastReadMessageId(member.getLastReadMessageId());
        entity.setLeftAt(member.getLeftAt());
        entity.setClearedAt(member.getClearedAt());
        entity.setMuted(member.isMuted());
        entity.setCreatedAt(member.getCreatedAt());
        entity.setCreatedBy(member.getCreatedBy());
        entity.setUpdatedAt(member.getUpdatedAt());
        entity.setUpdatedBy(member.getUpdatedBy());
        return entity;
    }
}
