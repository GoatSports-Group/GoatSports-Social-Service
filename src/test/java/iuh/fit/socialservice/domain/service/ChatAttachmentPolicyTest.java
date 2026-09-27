package iuh.fit.socialservice.domain.service;

import iuh.fit.socialservice.domain.enums.AttachmentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChatAttachmentPolicyTest {
    private final UUID sender = UUID.randomUUID();

    private ChatAttachmentPolicy.Item image(String key) {
        return new ChatAttachmentPolicy.Item(key, AttachmentType.IMAGE, 1024L);
    }

    @Test
    void acceptsTheSendersOwnTemporaryOrPromotedImages() {
        assertThat(ChatAttachmentPolicy.violation(List.of(
                image("temp/chat-messages/" + sender + "/a.jpg"),
                image("chat-messages/" + sender + "/b.jpg")), sender)).isNull();
        assertThat(ChatAttachmentPolicy.violation(List.of(), sender)).isNull();
    }

    @Test
    void rejectsOtherPeoplesKeysOtherFoldersAndTraversal() {
        UUID other = UUID.randomUUID();
        assertThat(ChatAttachmentPolicy.violation(List.of(image("temp/chat-messages/" + other + "/a.jpg")), sender)).isNotNull();
        assertThat(ChatAttachmentPolicy.violation(List.of(image("temp/avatars/" + sender + "/a.jpg")), sender)).isNotNull();
        assertThat(ChatAttachmentPolicy.violation(List.of(image("chat-messages/" + sender + "/../" + other + "/a.jpg")), sender)).isNotNull();
    }

    @Test
    void rejectsNonImagesOversizedFilesAndTooManyImages() {
        String key = "temp/chat-messages/" + sender + "/a.pdf";
        assertThat(ChatAttachmentPolicy.violation(List.of(new ChatAttachmentPolicy.Item(key, AttachmentType.FILE, 10L)), sender))
                .contains("chỉ hỗ trợ gửi ảnh");
        assertThat(ChatAttachmentPolicy.violation(List.of(new ChatAttachmentPolicy.Item(key, AttachmentType.IMAGE,
                ChatAttachmentPolicy.MAX_BYTES + 1)), sender)).contains("10 MB");
        List<ChatAttachmentPolicy.Item> eleven = IntStream.range(0, 11)
                .mapToObj(i -> image("temp/chat-messages/" + sender + "/" + i + ".jpg")).toList();
        assertThat(ChatAttachmentPolicy.violation(eleven, sender)).contains("tối đa 10 ảnh");
    }

    @Test
    void imageOnlyMessagesGetAReadablePreview() {
        assertThat(ChatAttachmentPolicy.preview("", 3)).isEqualTo("Đã gửi 3 ảnh");
        assertThat(ChatAttachmentPolicy.preview(null, 1)).isEqualTo("Đã gửi một ảnh");
        assertThat(ChatAttachmentPolicy.preview("Xem nè", 2)).isEqualTo("Xem nè");
    }
}
