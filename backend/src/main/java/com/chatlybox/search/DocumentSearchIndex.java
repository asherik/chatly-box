package com.chatlybox.search;

import java.util.List;
import java.util.UUID;

public interface DocumentSearchIndex {
  void indexChunk(UUID documentId, String title, String uri, int ordinal, String content);

  List<DocumentHit> search(String query, int limit);

  record DocumentHit(String documentId, String title, String uri, String excerpt) {}
}
