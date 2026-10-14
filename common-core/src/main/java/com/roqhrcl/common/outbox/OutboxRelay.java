package com.roqhrcl.common.outbox;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private static final String SELECT_UNPUBLISHED = """
             SELECT * FROM outbox
             WHERE published_at IS NULL
             ORDER BY id
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
             """;

    private final EntityManager entityManager;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    public OutboxRelay(EntityManager entityManager, KafkaTemplate<String, String> kafkaTemplate, TransactionTemplate transactionTemplate, int batchSize) {
        this.entityManager = entityManager;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:1000}")
    public void relay() {
        transactionTemplate.executeWithoutResult(status -> {
            @SuppressWarnings("unchecked")
            List<OutboxEvent> events = entityManager.createNativeQuery(SELECT_UNPUBLISHED, OutboxEvent.class)
                    .setParameter("limit", batchSize)
                    .getResultList();

            for (OutboxEvent event : events) {
                try {
                    kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                            .get(10, TimeUnit.SECONDS);
                    event.markPublished(Instant.now());
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    event.markFailed(e.toString());
                    log.warn("Failed to publish outbox event {}", event.getEventId(), e);
                    break;
                }
            }
        });
    }
}
