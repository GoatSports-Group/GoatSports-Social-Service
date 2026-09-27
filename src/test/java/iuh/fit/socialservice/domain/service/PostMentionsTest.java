package iuh.fit.socialservice.domain.service;

import iuh.fit.socialservice.domain.entity.Mention;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PostMentionsTest {
    private final UUID author = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void keepsOnlyNamesWrittenInTheContent() {
        List<Mention> kept = PostMentions.sanitize(
                List.of(new Mention(a, " Minh Anh "), new Mention(b, "Không viết ra")), "Chào @Minh Anh!", author, Set.of());

        assertThat(kept).containsExactly(new Mention(a, "Minh Anh"));
    }

    @Test
    void dropsTheAuthorBlockedPeopleDuplicatesAndBadRows() {
        UUID blocked = UUID.randomUUID();
        List<Mention> requested = new java.util.ArrayList<>(List.of(
                new Mention(author, "Tôi"), new Mention(blocked, "Chặn"), new Mention(a, "An"), new Mention(a, "An"),
                new Mention(null, "An"), new Mention(b, "")));
        requested.add(null);

        assertThat(PostMentions.sanitize(requested, "@Tôi @Chặn @An", author, Set.of(blocked)))
                .containsExactly(new Mention(a, "An"));
    }

    @Test
    void capsTheNumberOfMentions() {
        List<Mention> many = IntStream.range(0, 30).mapToObj(i -> new Mention(UUID.randomUUID(), "N" + i)).toList();
        String content = String.join(" ", many.stream().map(m -> "@" + m.name()).toList());

        assertThat(PostMentions.sanitize(many, content, author, Set.of())).hasSize(PostMentions.MAX_MENTIONS);
    }

    @Test
    void addedReturnsOnlyNewPeople() {
        assertThat(PostMentions.added(List.of(new Mention(a, "An")), List.of(new Mention(a, "An"), new Mention(b, "Bình"))))
                .containsExactly(b);
        assertThat(PostMentions.sanitize(List.of(new Mention(a, "An")), null, author, Set.of())).isEmpty();
    }
}
