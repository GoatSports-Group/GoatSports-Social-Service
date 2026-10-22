package iuh.fit.socialservice.infrastructure.persistence.mapper;

import iuh.fit.socialservice.domain.service.PostTags;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostHighlightDoc;
import iuh.fit.socialservice.domain.entity.PostHighlight;
import iuh.fit.socialservice.domain.entity.PlayerCall;
import iuh.fit.socialservice.domain.entity.Post;
import iuh.fit.socialservice.domain.entity.PostAttachment;
import iuh.fit.socialservice.infrastructure.persistence.entity.PlayerCallDoc;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostAttachmentDoc;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import org.springframework.stereotype.Component;

import iuh.fit.socialservice.domain.entity.Mention;
import iuh.fit.socialservice.infrastructure.persistence.entity.MentionDoc;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PostEntityMapper {
    public Post toDomain(PostEntity entity) {
        if (entity == null) return null;
        return Post.builder()
                .postId(toUuid(entity.getPostId()))
                .authorId(toUuid(entity.getAuthorId()))
                .content(entity.getContent())
                .visibility(entity.getVisibility())
                .status(entity.getStatus())
                .sport(entity.getSport())
                .sharedPostId(toUuid(entity.getSharedPostId()))
                .playerCall(toDomain(entity.getPlayerCall()))
                .highlight(toDomain(entity.getHighlight()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .publishedAt(entity.getPublishedAt())
                .attachments(entity.getAttachments() == null ? Collections.emptyList()
                        : entity.getAttachments().stream().map(this::toDomain).toList())
                .tags(entity.getTags() == null ? Collections.emptyList() : entity.getTags())
                .mentions(toMentions(entity.getMentions()))
                .build();
    }

    public PostEntity toEntity(Post domain) {
        if (domain == null) return null;
        return PostEntity.builder()
                .postId(toString(domain.getPostId()))
                .authorId(toString(domain.getAuthorId()))
                .content(domain.getContent())
                .visibility(domain.getVisibility())
                .status(domain.getStatus())
                .sport(domain.getSport())
                .sharedPostId(toString(domain.getSharedPostId()))
                .playerCall(toDocument(domain.getPlayerCall()))
                .highlight(toDocument(domain.getHighlight()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .publishedAt(domain.getPublishedAt())
                .attachments(domain.getAttachments() == null ? Collections.emptyList()
                        : domain.getAttachments().stream().map(this::toDocument).toList())
                .tags(domain.getTags() == null ? Collections.emptyList() : domain.getTags())
                .tagKeys(domain.getTags() == null ? new ArrayList<>()
                        : domain.getTags().stream().map(PostTags::key)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .mentions(toMentionDocs(domain.getMentions()))
                .build();
    }

    private PlayerCall toDomain(PlayerCallDoc doc) {
        if (doc == null) return null;
        return new PlayerCall(doc.getSport(), doc.getPlayFormat(), doc.getPlayDate(), toTime(doc.getStartTime()),
                toTime(doc.getEndTime()), doc.getLocation(), doc.getSlots(), doc.getSkillNote(), toUuid(doc.getClubId()),
                doc.getClubName(), doc.isFilled());
    }

    private PlayerCallDoc toDocument(PlayerCall call) {
        if (call == null) return null;
        return PlayerCallDoc.builder()
                .sport(call.sport()).playFormat(call.playFormat()).playDate(call.playDate())
                .startTime(call.startTime() == null ? null : call.startTime().toString())
                .endTime(call.endTime() == null ? null : call.endTime().toString())
                .location(call.location()).slots(call.slots()).skillNote(call.skillNote())
                .clubId(toString(call.clubId())).clubName(call.clubName()).filled(call.filled())
                .build();
    }

    private PostHighlight toDomain(PostHighlightDoc doc) {
        if (doc == null) return null;
        return new PostHighlight(doc.getKind(), toUuid(doc.getRefId()), doc.getTitle(), doc.getHeadline(),
                doc.getSubtitle(), doc.getResult(),
                doc.getSides() == null ? List.of() : doc.getSides().stream()
                        .map(side -> new PostHighlight.Side(side.getName(), side.getScore(), side.isWinner())).toList(),
                doc.getStats() == null ? List.of() : doc.getStats().stream()
                        .map(stat -> new PostHighlight.Stat(stat.getLabel(), stat.getValue())).toList(),
                toUuid(doc.getClubId()), doc.getClubName(),
                doc.getTagged() == null ? List.of() : doc.getTagged().stream().map(this::toUuid).toList());
    }

    private PostHighlightDoc toDocument(PostHighlight highlight) {
        if (highlight == null) return null;
        return PostHighlightDoc.builder()
                .kind(highlight.kind()).refId(toString(highlight.refId())).title(highlight.title())
                .headline(highlight.headline()).subtitle(highlight.subtitle()).result(highlight.result())
                .sides(highlight.sides().stream()
                        .map(side -> new PostHighlightDoc.SideDoc(side.name(), side.score(), side.winner()))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .stats(highlight.stats().stream()
                        .map(stat -> new PostHighlightDoc.StatDoc(stat.label(), stat.value()))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .clubId(toString(highlight.clubId())).clubName(highlight.clubName())
                .tagged(highlight.tagged().stream().map(UUID::toString)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .build();
    }

    private static LocalTime toTime(String value) {
        return value == null || value.isBlank() ? null : LocalTime.parse(value);
    }

    private PostAttachment toDomain(PostAttachmentDoc document) {
        return PostAttachment.builder()
                .attachmentId(toUuid(document.getAttachmentId()))
                .storageKey(document.getStorageKey())
                .type(document.getType())
                .displayOrder(document.getDisplayOrder())
                .build();
    }

    private PostAttachmentDoc toDocument(PostAttachment attachment) {
        return PostAttachmentDoc.builder()
                .attachmentId(toString(attachment.getAttachmentId()))
                .storageKey(attachment.getStorageKey())
                .type(attachment.getType())
                .displayOrder(attachment.getDisplayOrder())
                .build();
    }

    static List<Mention> toMentions(List<MentionDoc> docs) {
        if (docs == null) return new ArrayList<>();
        return docs.stream()
                .filter(doc -> doc.getUserId() != null)
                .map(doc -> new Mention(UUID.fromString(doc.getUserId()), doc.getName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static List<MentionDoc> toMentionDocs(List<Mention> mentions) {
        if (mentions == null) return new ArrayList<>();
        return mentions.stream()
                .map(mention -> new MentionDoc(mention.userId().toString(), mention.name()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
