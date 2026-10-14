package com.roqhrcl.common.idempotency;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ProcessedEventRecorder processedEventRecorder(EntityManager entityManager) {
        return new ProcessedEventRecorder(entityManager);
    }
}
