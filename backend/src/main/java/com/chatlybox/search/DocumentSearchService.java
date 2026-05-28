package com.chatlybox.search;

import com.chatlybox.documents.DocumentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class DocumentSearchService {
  private final DocumentSearchIndex index;
  private final DocumentRepository documents;

  public DocumentSearchService(DocumentSearchIndex index, DocumentRepository documents) {
    this.index = index;
    this.documents = documents;
  }

  public void indexChunk(UUID documentId, String title, String uri, int ordinal, String content) {
    index.indexChunk(documentId, title, uri, ordinal, content);
  }

  public List<DocumentSearchIndex.DocumentHit> search(String query, int limit) {
    return index.search(query, limit);
  }

  public Flux<DocumentSearchIndex.DocumentHit> searchReactive(String query, int limit) {
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

  public record ReindexResult(int documents, int chunks) {}
}
