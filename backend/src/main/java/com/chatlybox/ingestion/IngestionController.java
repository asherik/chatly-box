package com.chatlybox.ingestion;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {
  private final DocumentIngestionService service;

  public IngestionController(DocumentIngestionService service) {
    this.service = service;
  }

  @PostMapping("/sources/{sourceId}/sync")
  DocumentIngestionService.SyncResult sync(@PathVariable UUID sourceId) {
    return service.sync(sourceId);
  }
}
