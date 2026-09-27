package iuh.fit.socialservice.domain.service;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PostTagsTest {
    @Test
    void extractsLowercasedDistinctTagsInOrder() {
        assertThat(PostTags.extract("Đá #BóngĐá tối nay #keo_toi, rồi #bóngđá tiếp")).containsExactly("bóngđá", "keo_toi");
    }

    @Test
    void ignoresHashesInsideWordsEntitiesAndTooShortTags() {
        assertThat(PostTags.extract("abc#def &#123; #a # ##ok")).containsExactly("ok");
    }

    @Test
    void keepsVietnameseWrittenWithCombiningMarks() {
        String decomposed = Normalizer.normalize("#cầulông", Normalizer.Form.NFD);
        assertThat(PostTags.extract(decomposed)).singleElement()
                .satisfies(tag -> assertThat(Normalizer.normalize(tag, Normalizer.Form.NFC)).isEqualTo("cầulông"));
    }

    @Test
    void capsTheNumberOfTags() {
        String content = String.join(" ", IntStream.range(0, 15).mapToObj(i -> "#tag" + i).toList());
        assertThat(PostTags.extract(content)).hasSize(PostTags.MAX_TAGS);
    }

    @Test
    void normalizeAcceptsAFilterWithOrWithoutTheHash() {
        assertThat(PostTags.normalize(" #BóngĐá ")).isEqualTo("bóngđá");
        assertThat(PostTags.normalize("x")).isNull();
        assertThat(PostTags.normalize("a b")).isNull();
        assertThat(PostTags.normalize(null)).isNull();
    }
}
