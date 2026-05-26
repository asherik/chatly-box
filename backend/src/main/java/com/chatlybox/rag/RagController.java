package com.chatlybox.rag;

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

  public RagController(LlamaLib llamaLib) {
    this.llamaLib = llamaLib;
  }

  @PostMapping("/chat")
  Map<String, String> chat(@Valid @RequestBody ChatRequest request) {
    String json = """
        {"question": "%s", "chatId": "%s"}
        """.formatted(escape(request.question()), escape(request.chatId()));
    return Map.of("answer", llamaLib.ask(json));
  }

  @PostMapping("/index")
  Map<String, String> index(@Valid @RequestBody IndexRequest request) {
    String json = """
        {"sourceId": "%s", "uri": "%s"}
        """.formatted(escape(request.sourceId()), escape(request.uri()));
    return Map.of("result", llamaLib.index(json));
  }

  private static String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  record ChatRequest(@NotBlank String question, String chatId) {}
  record IndexRequest(@NotBlank String sourceId, @NotBlank String uri) {}
}
