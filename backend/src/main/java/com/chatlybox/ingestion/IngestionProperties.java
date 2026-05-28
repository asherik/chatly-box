package com.chatlybox.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatly.rag")
public record IngestionProperties(
    String lancedbUri,
    String embeddingModelPath,
    String chatModelPath,
    int chunkSize,
    int chunkOverlap,
    String ocrLanguage) {}
