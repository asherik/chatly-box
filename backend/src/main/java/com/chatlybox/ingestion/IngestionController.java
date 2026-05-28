package com.chatlybox.ingestion;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {
  private final DocumentIngestionService service;
  private final IngestionEventBus events;

  public IngestionController(DocumentIngestionService service, IngestionEventBus events) {
    this.service = service;
    this.events = events;
  }

  @PostMapping("/sources/{sourceId}/sync")
  DocumentIngestionService.SyncResult sync(@PathVariable UUID sourceId) {
    return service.sync(sourceId);
  }

  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  Flux<IngestionEventBus.IngestionEvent> events() {
    return events.stream();
  }
}
