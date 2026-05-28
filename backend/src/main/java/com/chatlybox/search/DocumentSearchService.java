package com.chatlybox.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.chatlybox.documents.DocumentRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class DocumentSearchService {
  private static final String INDEX = "chatly-documents";
  private final ElasticsearchClient elasticsearch;
  private final DocumentRepository documents;

  public DocumentSearchService(ElasticsearchClient elasticsearch, DocumentRepository documents) {
    this.elasticsearch = elasticsearch;
    this.documents = documents;
  }

  @PostConstruct
  void ensureIndex() {
    try {
      boolean exists = elasticsearch.indices().exists(request -> request.index(INDEX)).value();
      if (!exists) {
        elasticsearch.indices().create(request -> request
            .index(INDEX)
            .mappings(mapping -> mapping
                .properties("documentId", property -> property.keyword(keyword -> keyword))
                .properties("title", property -> property.text(text -> text))
                .properties("uri", property -> property.keyword(keyword -> keyword))
                .properties("ordinal", property -> property.integer(integer -> integer))
                .properties("content", property -> property.text(text -> text))));
      }
    } catch (IOException | ElasticsearchException error) {
      throw new IllegalStateException("Failed to bootstrap Elasticsearch index", error);
    }
  }

  public void indexChunk(UUID documentId, String title, String uri, int ordinal, String content) {
    try {
      elasticsearch.index(
          request ->
              request
                  .index(INDEX)
                  .id(documentId + ":" + ordinal)
                  .document(
                      Map.of(
                          "documentId", documentId.toString(),
                          "title", title,
                          "uri", uri,
                          "ordinal", ordinal,
                          "content", content)));
    } catch (IOException | ElasticsearchException error) {
      throw new IllegalStateException("Failed to index document chunk in Elasticsearch", error);
    }
  }

  public List<DocumentHit> search(String query, int limit) {
    try {
      SearchResponse<Map> response =
          elasticsearch.search(
              request ->
                  request
                      .index(INDEX)
                      .size(Math.max(1, limit))
                      .query(q -> q.multiMatch(m -> m.query(query).fields("title^2", "uri", "content"))),
              Map.class);

      return response.hits().hits().stream()
          .map(hit -> hit.source())
          .map(DocumentHit::from)
          .toList();
    } catch (IOException | ElasticsearchException error) {
      throw new IllegalStateException("Elasticsearch search failed", error);
    }
  }

  public Flux<DocumentHit> searchReactive(String query, int limit) {
    return Flux.defer(() -> Flux.fromIterable(search(query, limit))).subscribeOn(Schedulers.boundedElastic());
  }

  public ReindexResult reindexFromPostgres() {
    int documentsCount = 0;
    int chunksCount = 0;
    for (var document : documents.findTop50ByOrderByCreatedAtDesc()) {
      documentsCount += 1;
      for (var chunk : document.chunks) {
        indexChunk(document.id, document.title, document.uri, chunk.ordinal, chunk.content);
        chunksCount += 1;
      }
    }
    return new ReindexResult(documentsCount, chunksCount);
  }

  public record DocumentHit(String documentId, String title, String uri, String excerpt) {
    static DocumentHit from(Map source) {
      String content = String.valueOf(source.getOrDefault("content", ""));
      String excerpt = content.length() > 320 ? content.substring(0, 320) : content;
      return new DocumentHit(
          String.valueOf(source.getOrDefault("documentId", "")),
          String.valueOf(source.getOrDefault("title", "")),
          String.valueOf(source.getOrDefault("uri", "")),
          excerpt);
    }
  }

  public record ReindexResult(int documents, int chunks) {}
}
