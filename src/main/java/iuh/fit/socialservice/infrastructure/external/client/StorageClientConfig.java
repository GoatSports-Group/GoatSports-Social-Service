package iuh.fit.socialservice.infrastructure.external.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


public class StorageClientConfig {
    @Bean
    RequestInterceptor storageServiceSecurityInterceptor(
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        return template -> template.header("X-Gateway-Secret", gatewaySecret);
    }
}
