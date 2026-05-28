package com.chatlybox.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SettingsRequest(
        @NotBlank String embeddingModel,
        @NotBlank String chatModel,
        @Min(1) @Max(20) int topK,
        @Min(0) @Max(2) float temperature) {}