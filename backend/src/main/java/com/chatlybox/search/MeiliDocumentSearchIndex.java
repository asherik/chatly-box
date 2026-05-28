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
  public void indexChunk(UUID documentId, String title, String uri, int ordinal, String content) {
    ensureIndex();
    send(
        "POST",
        "/indexes/" + index() + "/documents",
        List.of(
            Map.of(
                "id", documentId + "_" + ordinal,
                "documentId", documentId.toString(),
                "title", title,
                "uri", uri,
                "ordinal", ordinal,
                "content", content)));
  }

  @Override
  public List<DocumentHit> search(String query, int limit) {
    ensureIndex();
    JsonNode response =
        send(
            "POST",
            "/indexes/" + index() + "/search",
            Map.of("q", query, "limit", Math.max(1, limit), "attributesToCrop", List.of("content")));
    return response.path("hits").findValuesAsText("id").isEmpty()
        ? List.of()
        : streamHits(response);
  }

  private List<DocumentHit> streamHits(JsonNode response) {
    return java.util.stream.StreamSupport.stream(response.path("hits").spliterator(), false)
        .map(hit -> new DocumentHit(
            hit.path("documentId").asText(),
            hit.path("title").asText(),
            hit.path("uri").asText(),
            excerpt(hit.path("content").asText())))
        .toList();
  }

  private void ensureIndex() {
    if (!indexReady.compareAndSet(false, true)) {
      return;
    }
    send("POST", "/indexes", Map.of("uid", index(), "primaryKey", "id"), 400, 409);
    send("PATCH", "/indexes/" + index() + "/settings/searchable-attributes", List.of("title", "uri", "content"));
  }

  private JsonNode send(String method, String path, Object body) {
    return send(method, path, body, new int[0]);
  }

  private JsonNode send(String method, String path, Object body, int... allowedErrorStatuses) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(trimSlash(properties.meiliUrl()) + path))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + properties.meiliMasterKey())
              .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400 && !isAllowed(response.statusCode(), allowedErrorStatuses)) {
        throw new IllegalStateException("Meilisearch request failed: " + response.statusCode() + " " + response.body());
      }
      return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
    } catch (Exception error) {
      throw new IllegalStateException("Meilisearch request failed", error);
    }
  }

  private static boolean isAllowed(int status, int[] allowedStatuses) {
    for (int allowed : allowedStatuses) {
      if (status == allowed) {
        return true;
      }
    }
    return false;
  }

  private String index() {
    return properties.index();
  }

  private static String trimSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static String excerpt(String content) {
    return content.length() > 320 ? content.substring(0, 320) : content;
  }
}
