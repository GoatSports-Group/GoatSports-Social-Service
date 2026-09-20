package iuh.fit.socialservice.application.port.external;

import java.util.List;

public interface StorageKeyPromoter {
    List<String> promote(List<String> storageKeys, String correlationId);
}
