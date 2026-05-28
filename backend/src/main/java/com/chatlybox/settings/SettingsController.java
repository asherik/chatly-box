package com.chatlybox.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
  private final JdbcTemplate jdbc;

  public SettingsController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  SettingsResponse get() {
    Map<String, String> values = jdbc.query(
        "select key, value from model_setting",
        rs -> {
          java.util.HashMap<String, String> rows = new java.util.HashMap<>();
          while (rs.next()) {
            rows.put(rs.getString("key"), rs.getString("value"));
          }
          return rows;
        });
    return SettingsResponse.from(values);
  }

  @PutMapping
  SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
    upsert("embeddingModel", request.embeddingModel());
    upsert("chatModel", request.chatModel());
    upsert("topK", Integer.toString(request.topK()));
    upsert("temperature", Float.toString(request.temperature()));
    return get();
  }

  private void upsert(String key, String value) {
    jdbc.update(
        """
        insert into model_setting(key, value, updated_at)
        values (?, ?, ?)
        on conflict (key) do update set value = excluded.value, updated_at = excluded.updated_at
        """,
        key,
        value,
        Timestamp.from(Instant.now()));
  }

  record SettingsRequest(
      @NotBlank String embeddingModel,
      @NotBlank String chatModel,
      @Min(1) @Max(20) int topK,
      @Min(0) @Max(2) float temperature) {}

  record SettingsResponse(
      String runtimeProvider,
      String embeddingModel,
      String chatModel,
      int topK,
      float temperature) {
    static SettingsResponse from(Map<String, String> values) {
      return new SettingsResponse(
          "llama.cpp",
          values.getOrDefault("embeddingModel", "models/embedding.gguf"),
          values.getOrDefault("chatModel", "models/chat.gguf"),
          Integer.parseInt(values.getOrDefault("topK", "6")),
          Float.parseFloat(values.getOrDefault("temperature", "0.2")));
    }
  }
}
