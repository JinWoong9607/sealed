package com.roqhrcl.sealed;

import com.roqhrcl.common.event.EventEnvelope;
import com.roqhrcl.common.outbox.OutboxWriter;
import com.roqhrcl.sealed.config.KafkaTopicConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class OutboxRelayTest {

    @Autowired
    OutboxWriter outboxWriter;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConsumerFactory<String, String> consumerFactory;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM outbox");
    }

    @Test
    void relayPublishesCommittedEventToKafka() {
        EventEnvelope<Map<String, String>> envelope = EventEnvelope.of("SealedCreated", 1, "sealed-42", Map.of("sealedId", "sealed-42"));

        transactionTemplate.executeWithoutResult(status ->
                outboxWriter.write(KafkaTopicConfig.SEALED_EVENTS, envelope));

        Properties overrides = new Properties();
        overrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = consumerFactory.createConsumer("outbox-relay-test", null, null, overrides)) {
            consumer.subscribe(List.of(KafkaTopicConfig.SEALED_EVENTS));

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, KafkaTopicConfig.SEALED_EVENTS, Duration.ofSeconds(10));

            assertThat(record.key()).isEqualTo("sealed-42");
            assertThat(record.value()).contains(envelope.eventId().toString());
        }

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Timestamp publishedAt = jdbcTemplate.queryForObject("SELECT published_at FROM outbox WHERE event_id = ?", Timestamp.class, envelope.eventId());
            assertThat(publishedAt).isNotNull();
        });
    }
}
