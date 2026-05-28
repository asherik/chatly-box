package com.chatlybox.search;

import java.util.List;
import java.util.UUID;

public interface DocumentSearchIndex {
  void upsertChunks(List<SearchableDocumentChunk> chunks);

  void deleteDocument(UUID documentId);

  void reset();

  List<DocumentHit> search(String query, int limit);

  record DocumentHit(
      String chunkId,
      String documentId,
      String sourceId,
      String title,
      String uri,
      int ordinal,
      String excerpt,
      double score) {}
}
