package com.roqhrcl.sealed;

import com.roqhrcl.common.idempotency.ProcessedEventRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(properties = "outbox.relay.enabled=false")
@Import(TestcontainersConfiguration.class)
public class ProcessedEventRecorderTest {

    @Autowired
    ProcessedEventRecorder recorder;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM processed_event");
    }

    @Test
    void firstTimeIsTrueAndSecondTimeIsFalse() {
        assertThat(markInTransaction("payment-request", eventId)).isTrue();
        assertThat(markInTransaction("payment-request", eventId)).isFalse();
    }

    @Test
    void differentConsumerIsTreatedSeparately() {
        assertThat(markInTransaction("payment-request", eventId)).isTrue();
        assertThat(markInTransaction("statistics", eventId)).isTrue();
    }

    @Test
    void rolledBackMarkCanBeRetried() {
        transactionTemplate.executeWithoutResult(status -> {
            recorder.markProcessed("payment-request", eventId);
            status.setRollbackOnly();
        });

        assertThat(markInTransaction("payment-request", eventId)).isTrue();
    }

    @Test
    void rejectsMarkWithoutTransaction() {
        assertThatThrownBy(() -> recorder.markProcessed("payment-request", eventId))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private boolean markInTransaction(String consumer, UUID eventId) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> recorder.markProcessed(consumer, eventId)));
    }
}
