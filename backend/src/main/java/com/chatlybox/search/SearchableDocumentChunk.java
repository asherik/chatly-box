package com.chatlybox.search;

import java.util.Map;
import java.util.UUID;

public record SearchableDocumentChunk(
    String id,
    UUID chunkId,
    UUID documentId,
    UUID sourceId,
    String title,
    String uri,
    int ordinal,
    String content,
    String checksum) {
  public Map<String, Object> toIndexDocument() {
    return Map.of(
        SearchDocumentFields.ID, id,
        SearchDocumentFields.CHUNK_ID, chunkId.toString(),
        SearchDocumentFields.DOCUMENT_ID, documentId.toString(),
        SearchDocumentFields.SOURCE_ID, sourceId.toString(),
        SearchDocumentFields.TITLE, title,
        SearchDocumentFields.URI, uri,
        SearchDocumentFields.ORDINAL, ordinal,
        SearchDocumentFields.CONTENT, content,
        SearchDocumentFields.CHECKSUM, checksum);
  }
}

