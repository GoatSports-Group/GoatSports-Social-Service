package iuh.fit.socialservice.infrastructure.external.kafka;

import iuh.fit.socialservice.application.port.external.SocialEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Dung khi tat producer (test, chay le): cac nghiep vu cong dong van chay, chi khong co thong bao. */
@Configuration
public class NoopSocialEventPublisher {
    @Bean
    @ConditionalOnMissingBean(SocialEventPublisher.class)
    SocialEventPublisher noopSocialEventPublisher() {
        return (eventType, receiverId, actorId, aggregateId, payload, dedupeKey) -> { };
    }
}
