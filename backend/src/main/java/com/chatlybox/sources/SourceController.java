package com.chatlybox.sources;

import com.chatlybox.documents.DocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sources")
public class SourceController {
  private final DocumentSourceRepository repository;
  private final DocumentRepository documents;
  private final ObjectMapper objectMapper;

  public SourceController(
      DocumentSourceRepository repository,
      DocumentRepository documents,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.documents = documents;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  List<SourceResponse> list() {
    return repository.findAll().stream().map(this::toResponse).toList();
  }

  @PostMapping
  SourceResponse create(@Valid @RequestBody SourceRequest request) throws Exception {
    DocumentSource source = new DocumentSource();
    source.id = UUID.randomUUID();
    source.name = request.name();
    source.type = request.type();
    source.config = objectMapper.writeValueAsString(request.config());
    return toResponse(repository.save(source));
  }

  private SourceResponse toResponse(DocumentSource source) {
    return new SourceResponse(
        source.id,
        source.name,
        source.type,
        source.status,
        source.lastError,
        source.lastSyncedAt,
        source.createdAt,
        documents.countBySourceId(source.id));
  }

  record SourceRequest(@NotBlank String name, @NotBlank String type, JsonNode config) {}

  record SourceResponse(
      UUID id,
      String name,
      String type,
      String status,
      String lastError,
      java.time.Instant lastSyncedAt,
      java.time.Instant createdAt,
      long documents) {}
}
