package com.chatlybox.search;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/search")
public class SearchController {
  private final DocumentSearchService searchService;

  public SearchController(DocumentSearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping
  List<DocumentSearchService.DocumentHit> search(
      @RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
    return searchService.search(q, limit);
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  Flux<DocumentSearchService.DocumentHit> stream(
      @RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
    return searchService.searchReactive(q, limit);
  }

  @PostMapping("/reindex")
  DocumentSearchService.ReindexResult reindex() {
    return searchService.reindexFromPostgres();
  }
}
