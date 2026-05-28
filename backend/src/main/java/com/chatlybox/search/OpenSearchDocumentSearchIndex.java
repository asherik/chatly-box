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
  public void indexChunk(UUID documentId, String title, String uri, int ordinal, String content) {
    ensureIndex();
    send(
        "PUT",
        "/" + index() + "/_doc/" + documentId + ":" + ordinal,
        Map.of(
            "documentId", documentId.toString(),
            "title", title,
            "uri", uri,
            "ordinal", ordinal,
            "content", content));
  }

  @Override
  public List<DocumentHit> search(String query, int limit) {
    ensureIndex();
    JsonNode response =
        send(
            "POST",
            "/" + index() + "/_search",
            Map.of(
                "size", Math.max(1, limit),
                "query",
                    Map.of(
                        "multi_match",
                        Map.of("query", query, "fields", List.of("title^2", "uri", "content")))));
    return java.util.stream.StreamSupport.stream(response.path("hits").path("hits").spliterator(), false)
        .map(hit -> hit.path("_source"))
        .map(source -> new DocumentHit(
            source.path("documentId").asText(),
            source.path("title").asText(),
            source.path("uri").asText(),
            excerpt(source.path("content").asText())))
        .toList();
  }

  private void ensureIndex() {
    if (!indexReady.compareAndSet(false, true)) {
      return;
    }
    send(
        "PUT",
        "/" + index(),
        Map.of(
            "mappings",
            Map.of(
                "properties",
                Map.of(
                    "documentId", Map.of("type", "keyword"),
                    "title", Map.of("type", "text"),
                    "uri", Map.of("type", "keyword"),
                    "ordinal", Map.of("type", "integer"),
                    "content", Map.of("type", "text")))),
        400);
  }

  private JsonNode send(String method, String path, Object body) {
    return send(method, path, body, new int[0]);
  }

  private JsonNode send(String method, String path, Object body, int... allowedErrorStatuses) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(trimSlash(properties.opensearchUrl()) + path))
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
      if (properties.opensearchUsername() != null && !properties.opensearchUsername().isBlank()) {
        builder.header("Authorization", "Basic " + basicAuth());
      }
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400 && !isAllowed(response.statusCode(), allowedErrorStatuses)) {
        throw new IllegalStateException("OpenSearch request failed: " + response.statusCode() + " " + response.body());
      }
      return response.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
    } catch (Exception error) {
      throw new IllegalStateException("OpenSearch request failed", error);
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

  private String basicAuth() {
    String raw = properties.opensearchUsername() + ":" + properties.opensearchPassword();
    return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
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
