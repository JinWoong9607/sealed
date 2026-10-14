package com.roqhrcl.sealed.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String SEALED_EVENTS = "sealed-events";

    @Bean
    NewTopic sealedEvents() {
        return TopicBuilder.name(SEALED_EVENTS)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
