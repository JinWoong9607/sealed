package com.roqhrcl.sealed;

import com.roqhrcl.common.event.EventEnvelope;
import com.roqhrcl.common.idempotency.ProcessedEventRecorder;
import com.roqhrcl.sealed.config.KafkaTopicConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "outbox.relay.enabled=false")
@Import(TestcontainersConfiguration.class)
public class DeadLetterTest {

    private static final String BROKEN_PAYLOAD = "this is not json";

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    ConsumerFactory<String, String> consumerFactory;

    @Autowired
    HandlingListener listener;

    @Test
    void brokenMessageGoesToDltAndNextMessageIsStillHandled() {
        EventEnvelope<Map<String, String>> good = EventEnvelope.of("SealedCreated", 1, "sealed-42", Map.of("sealedId", "sealed-42"));

        kafkaTemplate.send(KafkaTopicConfig.SEALED_EVENTS, "sealed-42", BROKEN_PAYLOAD);
        kafkaTemplate.send(KafkaTopicConfig.SEALED_EVENTS, "sealed-42", jsonMapper.writeValueAsString(good));

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() ->
                        assertThat(listener.handledCount()).isEqualTo(1));

        Properties overrides = new Properties();

        overrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = consumerFactory.createConsumer("dlt-reader", null , null, overrides)) {
            consumer.subscribe(List.of(KafkaTopicConfig.SEALED_EVENTS_DLT));

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, KafkaTopicConfig.SEALED_EVENTS_DLT, Duration.ofSeconds(10));

            assertThat(record.key()).isEqualTo("sealed-42");
            assertThat(record.value()).isEqualTo(BROKEN_PAYLOAD);
        }
    }

    @TestConfiguration
    static class ListenerConfig {
        @Bean
        HandlingListener handlingListener(ProcessedEventRecorder recorder, JsonMapper jsonMapper) {
            return new HandlingListener(recorder, jsonMapper);
        }
    }

    static class HandlingListener {
        private final AtomicInteger handled = new AtomicInteger();
        private final ProcessedEventRecorder recorder;
        private final JsonMapper jsonMapper;

        HandlingListener(ProcessedEventRecorder recorder, JsonMapper jsonMapper) {
            this.recorder = recorder;
            this.jsonMapper = jsonMapper;
        }

        @KafkaListener(topics = KafkaTopicConfig.SEALED_EVENTS, groupId = "dead-letter-test")
        @Transactional
        public void onMessage(String payload) {
            EventEnvelope<?> envelope =
                    jsonMapper.readValue(payload, EventEnvelope.class);
            if (!recorder.markProcessed("dead-letter-test", envelope.eventId())) {
                return;
            }
            handled.incrementAndGet();
        }

        int handledCount() {
            return handled.get();
        }

    }
}
