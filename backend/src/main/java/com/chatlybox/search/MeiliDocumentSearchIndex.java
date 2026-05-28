package com.chatlybox.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class MeiliDocumentSearchIndex implements DocumentSearchIndex {
  private static final int MAX_EXCERPT = 320;

  private final SearchProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient http;
  private final AtomicBoolean indexReady = new AtomicBoolean(false);

  MeiliDocumentSearchIndex(SearchProperties properties, ObjectMapper objectMapper, HttpClient http) {
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
    JsonNode task = send(
        "POST",
        "/indexes/" + index() + "/documents",
        chunks.stream().map(SearchableDocumentChunk::toIndexDocument).toList(),
        202);
    waitForTask(task.path("taskUid").asLong(-1));
  }

  @Override
  public void deleteDocument(UUID documentId) {
    ensureIndex();
    JsonNode task = send(
        "POST",
        "/indexes/" + index() + "/documents/delete",
        Map.of("filter", SearchDocumentFields.DOCUMENT_ID + " = \"" + documentId + "\""),
        202);
    waitForTask(task.path("taskUid").asLong(-1));
  }

  @Override
  public void reset() {
    JsonNode task = send("DELETE", "/indexes/" + index(), null, 202, 404);
    waitForTask(task.path("taskUid").asLong(-1));
    indexReady.set(false);
    ensureIndex();
  }

  @Override
  public List<DocumentHit> search(String query, int limit) {
    ensureIndex();
    JsonNode response = send(
        "POST",
        "/indexes/" + index() + "/search",
        Map.of(
            "q", query,
            "limit", Math.max(1, limit),
            "attributesToCrop", List.of(SearchDocumentFields.CONTENT),
            "cropLength", MAX_EXCERPT),
        200);

    return java.util.stream.StreamSupport.stream(response.path("hits").spliterator(), false)
        .map(this::toHit)
        .toList();
  }

  private DocumentHit toHit(JsonNode hit) {
    String content = hit.path("_formatted").path(SearchDocumentFields.CONTENT).asText(hit.path(SearchDocumentFields.CONTENT).asText());
    return new DocumentHit(
        hit.path(SearchDocumentFields.CHUNK_ID).asText(),
        hit.path(SearchDocumentFields.DOCUMENT_ID).asText(),
        hit.path(SearchDocumentFields.SOURCE_ID).asText(),
        hit.path(SearchDocumentFields.TITLE).asText(),
        hit.path(SearchDocumentFields.URI).asText(),
        hit.path(SearchDocumentFields.ORDINAL).asInt(),
        excerpt(stripTags(content)),
        hit.path("_rankingScore").asDouble(0));
  }

  private void ensureIndex() {
    if (!indexReady.compareAndSet(false, true)) {
      return;
    }
    JsonNode createTask = send("POST", "/indexes", Map.of("uid", index(), "primaryKey", SearchDocumentFields.ID), 202, 400, 409);
    waitForTask(createTask.path("taskUid").asLong(-1));
    waitForTask(send(
        "PATCH",
        "/indexes/" + index() + "/settings",
        Map.of(
            "searchableAttributes",
            List.of(SearchDocumentFields.TITLE, SearchDocumentFields.URI, SearchDocumentFields.CONTENT),
            "filterableAttributes",
            List.of(SearchDocumentFields.DOCUMENT_ID, SearchDocumentFields.SOURCE_ID, SearchDocumentFields.URI),
            "sortableAttributes",
            List.of(SearchDocumentFields.ORDINAL)),
        202).path("taskUid").asLong(-1));
  }

  private void waitForTask(long taskUid) {
    if (taskUid < 0) {
      return;
    }
    long deadline = System.currentTimeMillis() + 30_000;
    while (System.currentTimeMillis() < deadline) {
      JsonNode task = send("GET", "/tasks/" + taskUid, null, 200);
      String status = task.path("status").asText();
      if ("succeeded".equals(status)) {
        return;
      }
      if ("failed".equals(status) || "canceled".equals(status)) {
        throw new IllegalStateException("Meilisearch task failed: " + task);
      }
      sleep();
    }
    throw new IllegalStateException("Meilisearch task timed out: " + taskUid);
  }

  private JsonNode send(String method, String path, Object body, int... expectedStatuses) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(trimSlash(properties.meiliUrl()) + path))
          .header("Content-Type", "application/json")
          .method(method, body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
      if (properties.meiliMasterKey() != null && !properties.meiliMasterKey().isBlank()) {
        builder.header("Authorization", "Bearer " + properties.meiliMasterKey());
      }
      HttpResponse<String> response = sendWithRetry(builder.build());
      if (!isExpected(response.statusCode(), expectedStatuses)) {
        throw new IllegalStateException("Meilisearch request failed: " + response.statusCode() + " " + response.body());
      }
      return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
    } catch (Exception error) {
      throw new IllegalStateException("Meilisearch request failed", error);
    }
  }

  private String index() {
    return properties.index();
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
    throw lastError == null ? new IllegalStateException("Meilisearch request failed") : lastError;
  }

  private static boolean isExpected(int status, int[] expectedStatuses) {
    for (int expected : expectedStatuses) {
      if (status == expected) {
        return true;
      }
    }
    return false;
  }

  private static void sleep() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for Meilisearch task", error);
    }
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
