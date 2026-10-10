package com.roqhrcl.common.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String key,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, int version, String key, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, version, Instant.now(), key, payload);
    }
}