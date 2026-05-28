package com.chatlybox.rag;

import com.chatlybox.rag.dto.ChatRequest;
import com.chatlybox.rag.dto.IndexRequest;
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
}
