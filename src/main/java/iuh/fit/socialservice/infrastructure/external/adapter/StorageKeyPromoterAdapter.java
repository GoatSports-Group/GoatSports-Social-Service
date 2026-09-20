package iuh.fit.socialservice.infrastructure.external.adapter;

import iuh.fit.socialservice.application.port.external.StorageKeyPromoter;
import iuh.fit.socialservice.infrastructure.external.client.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageKeyPromoterAdapter implements StorageKeyPromoter {
    private static final String TEMP_PREFIX = "temp/";

    private final StorageClient storageClient;

    @Override
    public List<String> promote(List<String> storageKeys, String correlationId) {
        if (storageKeys == null || storageKeys.isEmpty()) return List.of();

        // Chi gui nhung khoa con nam trong temp/; khoa da promote roi gui lai se bi storage tu choi.
        List<StorageClient.FileMoveRequest> pending = storageKeys.stream()
                .filter(key -> key != null && key.startsWith(TEMP_PREFIX))
                .map(StorageClient.FileMoveRequest::new)
                .toList();
        if (pending.isEmpty()) return storageKeys;

        try {
            var confirmed = storageClient.confirmFile(pending);
            if (confirmed == null || confirmed.getData() == null
                    || confirmed.getData().size() != pending.size()) {
                log.warn("Storage tra ve so khoa khong khop khi xac nhan {} tep", pending.size());
                return storageKeys;
            }

            // Copy that su chay bat dong bo qua Kafka; confirm o tren chi kiem tep co that.
            storageClient.moveFile(new StorageClient.MoveFileRequest(pending, correlationId));

            return merge(storageKeys, confirmed.getData());
        } catch (RuntimeException exception) {
            // Mat anh con hon mat bai viet: giu khoa tam, bai van dang duoc va anh van hien
            // vi object chua bi doi cho. Lan sua bai sau se promote lai.
            log.warn("Không promote được tệp của {}: {}", correlationId, exception.getMessage());
            return storageKeys;
        }
    }

    /** Ghep khoa vinh vien tro lai dung vi tri cua khoa tam tuong ung. */
    private List<String> merge(List<String> original, List<String> permanentKeys) {
        List<String> result = new ArrayList<>(original.size());
        int cursor = 0;
        for (String key : original) {
            if (key != null && key.startsWith(TEMP_PREFIX)) {
                result.add(permanentKeys.get(cursor++));
            } else {
                result.add(key);
            }
        }
        return result;
    }
}
