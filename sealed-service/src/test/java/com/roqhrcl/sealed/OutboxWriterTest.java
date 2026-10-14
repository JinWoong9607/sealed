package com.roqhrcl.sealed;

import com.roqhrcl.common.event.EventEnvelope;
import com.roqhrcl.common.outbox.OutboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "outbox.relay.enabled=false")
@Import(TestcontainersConfiguration.class)
public class OutboxWriterTest {

    @Autowired
    OutboxWriter outboxWriter;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final EventEnvelope<Map<String, String>> envelope =
            EventEnvelope.of("SealedCreated", 1, "sealed-42", Map.of("sealedId", "sealed-42"));

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM outbox");
    }

    @Test
    void savesRowWhenTransactionCommits() {
        transactionTemplate.executeWithoutResult(status -> outboxWriter.write("sealed-events", envelope));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM outbox");
        assertThat(row.get("event_id")).isEqualTo(envelope.eventId());
        assertThat(row.get("aggregate_id")).isEqualTo("sealed-42");
        assertThat(row.get("topic")).isEqualTo("sealed-events");
        assertThat(row.get("published_at")).isNull();

        String sealedId = jdbcTemplate.queryForObject(
                "SELECT payload -> 'payload' ->> 'sealedId' FROM outbox", String.class);
        assertThat(sealedId).isEqualTo("sealed-42");
    }

    @Test
    void savesNothingWhenTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(status -> {
            outboxWriter.write("sealed-events", envelope);
            status.setRollbackOnly();
        });

        assertThat(countRows()).isZero();
    }

    @Test
    void rejectsWriteWithoutTransaction() {
        assertThatThrownBy(() -> outboxWriter.write("sealed-events", envelope))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(countRows()).isZero();
    }

    private int countRows() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM outbox", Integer.class);
    }
}
