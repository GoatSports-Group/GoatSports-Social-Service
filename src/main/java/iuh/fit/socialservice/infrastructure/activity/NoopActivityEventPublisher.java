package iuh.fit.socialservice.infrastructure.activity;

import iuh.fit.socialservice.application.port.external.ActivityEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tắt phát hành vi (test, chạy lẻ): cộng đồng vẫn chạy, tab Khám phá chỉ không được cá nhân hóa theo hành vi mới. */
@Configuration
public class NoopActivityEventPublisher {
    @Bean
    @ConditionalOnMissingBean(ActivityEventPublisher.class)
    ActivityEventPublisher noopActivityEventPublisher() {
        return (eventType, userId, payload) -> { };
    }
}
