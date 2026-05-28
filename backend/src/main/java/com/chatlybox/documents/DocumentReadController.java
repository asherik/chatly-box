package com.chatlybox.documents;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentReadController {
  private final DocumentRepository repository;

  public DocumentReadController(DocumentRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  List<DocumentSummary> list() {
    return repository.findTop50ByOrderByCreatedAtDesc().stream().map(DocumentSummary::from).toList();
  }

  @GetMapping("/{id}")
  DocumentDetails get(@PathVariable UUID id) {
    return repository.findById(id).map(DocumentDetails::from).orElseThrow();
  }

  record DocumentSummary(UUID id, String title, String uri, int chunks) {
    static DocumentSummary from(DocumentEntity document) {
      return new DocumentSummary(document.id, document.title, document.uri, document.chunks.size());
    }
  }

  record DocumentDetails(UUID id, String title, String uri, List<String> chunks) {
    static DocumentDetails from(DocumentEntity document) {
      return new DocumentDetails(
          document.id,
          document.title,
          document.uri,
          document.chunks.stream().map(chunk -> chunk.content).toList());
    }
  }
}
