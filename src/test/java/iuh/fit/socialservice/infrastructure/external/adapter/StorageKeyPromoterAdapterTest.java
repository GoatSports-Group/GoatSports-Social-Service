package iuh.fit.socialservice.infrastructure.external.adapter;

import iuh.fit.socialservice.application.dto.response.base.BaseResponse;
import iuh.fit.socialservice.infrastructure.external.client.StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageKeyPromoterAdapterTest {

    @Mock private StorageClient storageClient;

    private StorageKeyPromoterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StorageKeyPromoterAdapter(storageClient);
    }

    private BaseResponse<List<String>> ok(List<String> keys) {
        return new BaseResponse<>(200, keys, "ok", null);
    }

    @Test
    @DisplayName("khóa tạm được đổi sang khóa vĩnh viễn và đẩy lệnh copy")
    void temporaryKeysArePromotedAndMoved() {
        when(storageClient.confirmFile(anyList()))
                .thenReturn(ok(List.of("social-posts/u/a.png", "social-posts/u/b.png")));

        List<String> result = adapter.promote(
                List.of("temp/social-posts/u/a.png", "temp/social-posts/u/b.png"), "social-post");

        assertThat(result).containsExactly("social-posts/u/a.png", "social-posts/u/b.png");
        verify(storageClient).moveFile(any());
    }

    @Test
    @DisplayName("khóa đã vĩnh viễn thì không gọi storage lần nữa")
    void alreadyPromotedKeysAreLeftAlone() {
        List<String> keys = List.of("social-posts/u/a.png");

        assertThat(adapter.promote(keys, "social-post")).isEqualTo(keys);

        verify(storageClient, never()).confirmFile(anyList());
        verify(storageClient, never()).moveFile(any());
    }

    @Test
    @DisplayName("trộn khóa tạm và khóa cũ thì khóa mới về đúng vị trí của nó")
    void mixedKeysKeepTheirPositions() {
        when(storageClient.confirmFile(anyList())).thenReturn(ok(List.of("social-posts/u/new.png")));

        List<String> result = adapter.promote(
                List.of("social-posts/u/old.png", "temp/social-posts/u/new.png"), "social-post");

        assertThat(result).containsExactly("social-posts/u/old.png", "social-posts/u/new.png");
    }

    @Test
    @DisplayName("storage lỗi thì giữ khóa tạm, không làm hỏng việc đăng bài")
    void storageFailureKeepsOriginalKeys() {
        when(storageClient.confirmFile(anyList())).thenThrow(new IllegalStateException("storage down"));

        List<String> keys = List.of("temp/social-posts/u/a.png");

        assertThat(adapter.promote(keys, "social-post")).isEqualTo(keys);
        verify(storageClient, never()).moveFile(any());
    }

    @Test
    @DisplayName("storage trả về thiếu khóa thì không ghép bừa")
    void mismatchedResponseKeepsOriginalKeys() {
        when(storageClient.confirmFile(anyList())).thenReturn(ok(List.of("social-posts/u/a.png")));

        List<String> keys = List.of("temp/social-posts/u/a.png", "temp/social-posts/u/b.png");

        assertThat(adapter.promote(keys, "social-post")).isEqualTo(keys);
        verify(storageClient, never()).moveFile(any());
    }

    @Test
    @DisplayName("không có tệp nào thì không gọi storage")
    void emptyInputSkipsStorage() {
        assertThat(adapter.promote(List.of(), "social-post")).isEmpty();
        verify(storageClient, never()).confirmFile(anyList());
    }
}
