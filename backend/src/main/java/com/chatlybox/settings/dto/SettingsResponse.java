package com.chatlybox.settings.dto;

import java.util.Map;

public record SettingsResponse(
        String runtimeProvider,
        String embeddingModel,
        String chatModel,
        int topK,
        float temperature) {
    public static SettingsResponse from(Map<String, String> values) {
        return new SettingsResponse(
                "llama.cpp",
                values.getOrDefault("embeddingModel", "models/embedding.gguf"),
                values.getOrDefault("chatModel", "models/chat.gguf"),
                Integer.parseInt(values.getOrDefault("topK", "6")),
                Float.parseFloat(values.getOrDefault("temperature", "0.2")));
    }
}