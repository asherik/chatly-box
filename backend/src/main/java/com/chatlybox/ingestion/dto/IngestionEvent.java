package com.chatlybox.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

public record IngestionEvent(
        UUID sourceId,
        String stage,
        String message,
        int documents,
        int chunks,
        Instant timestamp) {
    public static IngestionEvent of(UUID sourceId, String stage, String message, int documents, int chunks) {
        return new IngestionEvent(sourceId, stage, message, documents, chunks, Instant.now());
    }
}
