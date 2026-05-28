package com.chatlybox.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String dbPath,
        @NotBlank String embeddingModelPath,
        @NotBlank String chatModelPath,
        @NotBlank String question,
        Integer topK,
        Integer maxTokens,
        Float temperature,
        Integer ctxSize,
        String systemPrompt) {}
