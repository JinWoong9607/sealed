package com.roqhrcl.common.outbox;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OutboxWriter outboxWriter(EntityManager entityManager, JsonMapper jsonMapper) {
        return new OutboxWriter(entityManager, jsonMapper);
    }
}
