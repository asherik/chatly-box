package com.chatlybox.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record IndexRequest(
        @NotBlank String dbPath,
        @NotBlank String embeddingModelPath,
        String documentId,
        @NotBlank String title,
        @NotBlank String uri,
        java.util.List<String> chunks,
        Integer ctxSize) {}
