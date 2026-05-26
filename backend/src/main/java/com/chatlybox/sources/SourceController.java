package com.chatlybox.sources;

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

  public SourceController(DocumentSourceRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  List<DocumentSource> list() {
    return repository.findAll();
  }

  @PostMapping
  DocumentSource create(@Valid @RequestBody SourceRequest request) {
    DocumentSource source = new DocumentSource();
    source.id = UUID.randomUUID();
    source.name = request.name();
    source.type = request.type();
    source.config = request.configJson();
    return repository.save(source);
  }

  record SourceRequest(@NotBlank String name, @NotBlank String type, @NotBlank String configJson) {}
}
