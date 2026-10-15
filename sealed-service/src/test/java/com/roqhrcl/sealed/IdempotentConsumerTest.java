package com.roqhrcl.sealed;

import com.roqhrcl.common.event.EventEnvelope;
import com.roqhrcl.common.idempotency.ProcessedEventRecorder;
import com.roqhrcl.sealed.config.KafkaTopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "outbox.relay.enabled=false")
@Import(TestcontainersConfiguration.class)
public class IdempotentConsumerTest {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    CountingListener listener;

    @Test
    void duplicateEventIsHandledOnlyOnce() {
        EventEnvelope<Map<String, String>> envelope = EventEnvelope.of("SealedCreated", 1,"sealed-42", Map.of("sealedId", "sealed-42"));

        String payload = jsonMapper.writeValueAsString(envelope);

        kafkaTemplate.send(KafkaTopicConfig.SEALED_EVENTS, "sealed-42", payload);
        kafkaTemplate.send(KafkaTopicConfig.SEALED_EVENTS, "sealed-42", payload);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(listener.receivedCount()).isEqualTo(2));
        assertThat(listener.handledCount()).isEqualTo(1);
    }

    @TestConfiguration
    static class ListenerConfig {
        @Bean
        CountingListener countingListener(ProcessedEventRecorder recorder, JsonMapper jsonMapper) {
            return new CountingListener(recorder, jsonMapper);
        }
    }

    static class CountingListener {

        int receivedCount() {
            return received.get();
        }

        int handledCount() {
            return handled.get();
        }
        private final AtomicInteger received = new AtomicInteger();
        private final AtomicInteger handled = new AtomicInteger();
        private final ProcessedEventRecorder recorder;
        private final JsonMapper jsonMapper;

        CountingListener(ProcessedEventRecorder recorder, JsonMapper jsonMapper) {
            this.recorder = recorder;
            this.jsonMapper = jsonMapper;
        }

        @KafkaListener(topics = KafkaTopicConfig.SEALED_EVENTS, groupId = "idempotency-test")
        @Transactional
        public void onMessage(String payload) {
            received.incrementAndGet();
            EventEnvelope<?> envelope = jsonMapper.readValue(payload, EventEnvelope.class);

            if (!recorder.markProcessed("idempotency-test", envelope.eventId())) {
                return;
            }
            handled.incrementAndGet();
        }

    }
}
