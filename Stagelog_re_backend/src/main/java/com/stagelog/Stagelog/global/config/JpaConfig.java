package com.stagelog.Stagelog.global.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaConfig {

    /**
     * BaseEntity가 OffsetDateTime을 쓰므로 auditing의 createdAt/updatedAt도 OffsetDateTime으로 강제.
     * Spring Data의 DefaultAuditableBeanWrapperFactory는 OffsetDateTime을 변환 대상으로 지원하지 않으므로
     * DateTimeProvider가 OffsetDateTime을 직접 반환해야 한다.
     */
    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
