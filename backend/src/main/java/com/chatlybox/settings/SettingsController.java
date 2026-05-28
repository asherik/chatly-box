package com.chatlybox.settings;

import com.chatlybox.settings.dto.SettingsRequest;
import com.chatlybox.settings.dto.SettingsResponse;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

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

}
