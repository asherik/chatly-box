package com.chatlybox.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatlybox.nativebridge.LlamaLib;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {
  private final LlamaLib llamaLib;
  private final ObjectMapper objectMapper;

  public RagController(LlamaLib llamaLib, ObjectMapper objectMapper) {
    this.llamaLib = llamaLib;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/chat")
  Map<String, String> chat(@Valid @RequestBody ChatRequest request) {
    return Map.of("answer", llamaLib.ask(toJson(request)));
  }

  @PostMapping("/index")
  Map<String, String> index(@Valid @RequestBody IndexRequest request) {
    return Map.of("result", llamaLib.index(toJson(request)));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception error) {
      throw new IllegalArgumentException("Invalid native request payload", error);
    }
  }

  record ChatRequest(
      @NotBlank String dbPath,
      @NotBlank String embeddingModelPath,
      @NotBlank String chatModelPath,
      @NotBlank String question,
      Integer topK,
      Integer maxTokens,
      Float temperature,
      Integer ctxSize,
      String systemPrompt) {}

  record IndexRequest(
      @NotBlank String dbPath,
      @NotBlank String embeddingModelPath,
      String documentId,
      @NotBlank String title,
      @NotBlank String uri,
      java.util.List<String> chunks,
      Integer ctxSize) {}
}
