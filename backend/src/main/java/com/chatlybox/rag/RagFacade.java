package com.chatlybox.rag;

import com.chatlybox.ingestion.IngestionProperties;
import com.chatlybox.nativebridge.LlamaLib;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

@Service
public class RagFacade {
  private final LlamaLib llamaLib;
  private final ObjectMapper objectMapper;
  private final IngestionProperties properties;
  private final CircuitBreakerFactory<?, ?> circuitBreakers;

  public RagFacade(
      LlamaLib llamaLib,
      ObjectMapper objectMapper,
      IngestionProperties properties,
      CircuitBreakerFactory<?, ?> circuitBreakers) {
    this.llamaLib = llamaLib;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.circuitBreakers = circuitBreakers;
  }

  public String ask(String question) {
    return circuitBreakers
        .create("native-rag")
        .run(
            () -> llamaLib.ask(toJson(question)),
            error -> "RAG native engine is temporarily unavailable: " + error.getMessage());
  }

  private String toJson(String question) {
    try {
      return objectMapper.writeValueAsString(
          Map.of(
              "dbPath", properties.lancedbUri(),
              "embeddingModelPath", properties.embeddingModelPath(),
              "chatModelPath", properties.chatModelPath(),
              "question", question,
              "topK", 6));
    } catch (Exception error) {
      throw new IllegalArgumentException("Invalid RAG request", error);
    }
  }
}
