package com.chatlybox.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class OpenSearchDocumentSearchIndex implements DocumentSearchIndex {
  private static final int MAX_EXCERPT = 320;

  private final SearchProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient http;
  private final AtomicBoolean indexReady = new AtomicBoolean(false);

  OpenSearchDocumentSearchIndex(SearchProperties properties, ObjectMapper objectMapper, HttpClient http) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.http = http;
  }

  @Override
  public void upsertChunks(List<SearchableDocumentChunk> chunks) {
    if (chunks.isEmpty()) {
      return;
    }
    ensureIndex();
    StringBuilder bulk = new StringBuilder();
    for (SearchableDocumentChunk chunk : chunks) {
      bulk.append(json(Map.of("index", Map.of("_index", index(), "_id", chunk.id())))).append('\n');
      bulk.append(json(chunk.toIndexDocument())).append('\n');
    }
    JsonNode response = sendNdJson("POST", "/_bulk?refresh=true", bulk.toString(), 200);
    if (response.path("errors").asBoolean(false)) {
      throw new IllegalStateException("OpenSearch bulk indexing failed: " + response);
    }
  }

  @Override
  public void deleteDocument(UUID documentId) {
    ensureIndex();
    send(
        "POST",
        "/" + index() + "/_delete_by_query?conflicts=proceed&refresh=true",
        Map.of("query", Map.of("term", Map.of(SearchDocumentFields.DOCUMENT_ID, documentId.toString()))),
        200);
  }

  @Override
  public void reset() {
    send("DELETE", "/" + index(), null, 200, 404);
    indexReady.set(false);
    ensureIndex();
  }

  @Override
  public List<DocumentHit> search(String query, int limit) {
    ensureIndex();
    JsonNode response = send(
        "POST",
        "/" + index() + "/_search",
        Map.of(
            "size", Math.max(1, limit),
            "query",
            Map.of(
                "multi_match",
                Map.of(
                    "query", query,
                    "fields",
                    List.of(
                        SearchDocumentFields.TITLE + "^2",
                        SearchDocumentFields.URI,
                        SearchDocumentFields.CONTENT))),
            "highlight",
            Map.of(
                "fields",
                Map.of(SearchDocumentFields.CONTENT, Map.of("fragment_size", MAX_EXCERPT, "number_of_fragments", 1)))),
        200);

    return java.util.stream.StreamSupport.stream(response.path("hits").path("hits").spliterator(), false)
        .map(this::toHit)
        .toList();
  }

  private DocumentHit toHit(JsonNode hit) {
    JsonNode source = hit.path("_source");
    JsonNode highlight = hit.path("highlight").path(SearchDocumentFields.CONTENT);
    String content = highlight.isArray() && !highlight.isEmpty()
        ? highlight.get(0).asText()
        : source.path(SearchDocumentFields.CONTENT).asText();
    return new DocumentHit(
        source.path(SearchDocumentFields.CHUNK_ID).asText(),
        source.path(SearchDocumentFields.DOCUMENT_ID).asText(),
        source.path(SearchDocumentFields.SOURCE_ID).asText(),
        source.path(SearchDocumentFields.TITLE).asText(),
        source.path(SearchDocumentFields.URI).asText(),
        source.path(SearchDocumentFields.ORDINAL).asInt(),
        excerpt(stripTags(content)),
        hit.path("_score").asDouble(0));
  }

  private void ensureIndex() {
    if (!indexReady.compareAndSet(false, true)) {
      return;
    }
    if (send("HEAD", "/" + index(), null, 200, 404).path("_status").asInt() == 200) {
      return;
    }
    send(
        "PUT",
        "/" + index(),
        Map.of(
            "settings",
            Map.of("index", Map.of("number_of_shards", 1, "number_of_replicas", 0)),
            "mappings",
            Map.of(
                "dynamic", "strict",
                "properties",
                Map.of(
                    SearchDocumentFields.ID, Map.of("type", "keyword"),
                    SearchDocumentFields.CHUNK_ID, Map.of("type", "keyword"),
                    SearchDocumentFields.DOCUMENT_ID, Map.of("type", "keyword"),
                    SearchDocumentFields.SOURCE_ID, Map.of("type", "keyword"),
                    SearchDocumentFields.TITLE, Map.of("type", "text"),
                    SearchDocumentFields.URI, Map.of("type", "keyword"),
                    SearchDocumentFields.ORDINAL, Map.of("type", "integer"),
                    SearchDocumentFields.CONTENT, Map.of("type", "text"),
                    SearchDocumentFields.CHECKSUM, Map.of("type", "keyword")))),
        200);
  }

  private JsonNode send(String method, String path, Object body, int... expectedStatuses) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(trimSlash(properties.opensearchUrl()) + path))
          .header("Content-Type", "application/json")
          .method(method, body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(json(body)));
      addAuth(builder);
      HttpResponse<String> response = sendWithRetry(builder.build());
      if (!isExpected(response.statusCode(), expectedStatuses)) {
        throw new IllegalStateException("OpenSearch request failed: " + response.statusCode() + " " + response.body());
      }
      if (method.equals("HEAD")) {
        return objectMapper.createObjectNode().put("_status", response.statusCode());
      }
      return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
    } catch (Exception error) {
      throw new IllegalStateException("OpenSearch request failed", error);
    }
  }

  private JsonNode sendNdJson(String method, String path, String body, int... expectedStatuses) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(trimSlash(properties.opensearchUrl()) + path))
          .header("Content-Type", "application/x-ndjson")
          .method(method, HttpRequest.BodyPublishers.ofString(body));
      addAuth(builder);
      HttpResponse<String> response = sendWithRetry(builder.build());
      if (!isExpected(response.statusCode(), expectedStatuses)) {
        throw new IllegalStateException("OpenSearch request failed: " + response.statusCode() + " " + response.body());
      }
      return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
    } catch (Exception error) {
      throw new IllegalStateException("OpenSearch request failed", error);
    }
  }

  private void addAuth(HttpRequest.Builder builder) {
    if (properties.opensearchUsername() != null && !properties.opensearchUsername().isBlank()) {
      builder.header("Authorization", "Basic " + basicAuth());
    }
  }

  private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
    Exception lastError = null;
    for (int attempt = 0; attempt < 20; attempt += 1) {
      try {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (java.net.ConnectException error) {
        lastError = error;
        sleep();
      }
    }
    throw lastError == null ? new IllegalStateException("OpenSearch request failed") : lastError;
  }

  private static void sleep() {
    try {
      Thread.sleep(250);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for OpenSearch", error);
    }
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception error) {
      throw new IllegalArgumentException("Invalid OpenSearch payload", error);
    }
  }

  private String basicAuth() {
    String raw = properties.opensearchUsername() + ":" + properties.opensearchPassword();
    return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private String index() {
    return properties.index();
  }

  private static boolean isExpected(int status, int[] expectedStatuses) {
    for (int expected : expectedStatuses) {
      if (status == expected) {
        return true;
      }
    }
    return false;
  }

  private static String trimSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static String excerpt(String content) {
    return content.length() > MAX_EXCERPT ? content.substring(0, MAX_EXCERPT) : content;
  }

  private static String stripTags(String value) {
    return value.replaceAll("<[^>]+>", "");
  }
}
