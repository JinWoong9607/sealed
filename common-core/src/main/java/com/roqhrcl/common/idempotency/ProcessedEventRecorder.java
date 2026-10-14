package com.roqhrcl.common.idempotency;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ProcessedEventRecorder {

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO processed_event (consumer, event_id)
            VALUES (:consumer, :eventId)
            ON CONFLICT DO NOTHING
            """;

    private final EntityManager entityManager;

    public ProcessedEventRecorder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String consumer, UUID eventId) {
        int inserted = entityManager.createNativeQuery(INSERT_IF_ABSENT)
                .setParameter("consumer", consumer)
                .setParameter("eventId", eventId)
                .executeUpdate();
        return inserted == 1;
    }
}
