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

  public void indexDocument(com.chatlybox.documents.DocumentEntity document) {
    index.deleteDocument(document.id);
    index.upsertChunks(document.chunks.stream()
        .map(chunk -> new SearchableDocumentChunk(
            document.id + "_" + chunk.ordinal,
            chunk.id,
            document.id,
            document.sourceId,
            document.title,
            document.uri,
            chunk.ordinal,
            chunk.content,
            document.checksum))
        .toList());
  }

  public List<DocumentSearchIndex.DocumentHit> search(String query, int limit) {
    return index.search(query, limit);
  }

  public Flux<DocumentSearchIndex.DocumentHit> searchReactive(String query, int limit) {
    return Flux.defer(() -> Flux.fromIterable(search(query, limit))).subscribeOn(Schedulers.boundedElastic());
  }

  public ReindexResult reindexFromPostgres() {
    index.reset();
    int documentsCount = 0;
    int chunksCount = 0;
    for (var document : documents.findAllByOrderByCreatedAtDesc()) {
      documentsCount += 1;
      indexDocument(document);
      chunksCount += document.chunks.size();
    }
    return new ReindexResult(documentsCount, chunksCount);
  }

  public record ReindexResult(int documents, int chunks) {}
}
