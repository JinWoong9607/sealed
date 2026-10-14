package com.roqhrcl.common.outbox;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;
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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnBooleanProperty(name = "outbox.relay.enabled", matchIfMissing = true)
    @EnableScheduling
    static class RelayConfiguration {
        @Bean
        @ConditionalOnMissingBean
        OutboxRelay outboxRelay(EntityManager entityManager, KafkaTemplate<String, String> kafkaTemplate, TransactionTemplate transactionTemplate, @Value("${outbox.relay.batch-size:100}") int batchSize) {
            return new OutboxRelay(entityManager, kafkaTemplate, transactionTemplate, batchSize);
        }
    }
}
