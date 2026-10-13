package com.roqhrcl.common.outbox;

import com.roqhrcl.common.event.EventEnvelope;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

public class OutboxWriter {

    private final EntityManager entityManager;
    private JsonMapper jsonMapper;
    public OutboxWriter(EntityManager entityManager, JsonMapper jsonMapper) {
        this.entityManager = entityManager;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String topic, EventEnvelope<?> envelope) {
        String payload = jsonMapper.writeValueAsString(envelope);
        OutboxEvent event = new OutboxEvent(
                envelope.eventId(),
                envelope.key(),
                envelope.eventType(),
                topic,
                payload,
                envelope.occurredAt());
        entityManager.persist(event);
    }
}
