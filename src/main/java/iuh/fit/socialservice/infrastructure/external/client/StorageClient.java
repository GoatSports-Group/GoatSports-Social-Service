package iuh.fit.socialservice.infrastructure.external.client;

import iuh.fit.socialservice.application.dto.response.base.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "GOATSPORTS-STORAGE-SERVICE",
        url = "${storage.service.url}",
        path = "/internal/files",
        configuration = StorageClientConfig.class
)
public interface StorageClient {

    @PostMapping("/confirm")
    BaseResponse<List<String>> confirmFile(@RequestBody List<FileMoveRequest> requests);

    @PostMapping("/move")
    BaseResponse<Void> moveFile(@RequestBody MoveFileRequest request);

    record FileMoveRequest(String tempKey) { }

    record MoveFileRequest(List<FileMoveRequest> requests, String correlationId) { }
}
