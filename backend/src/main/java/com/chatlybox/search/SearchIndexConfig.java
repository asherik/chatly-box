package com.chatlybox.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class SearchIndexConfig {
  @Bean
  HttpClient searchHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  @Bean
  DocumentSearchIndex documentSearchIndex(
      SearchProperties properties,
      ObjectMapper objectMapper,
      HttpClient searchHttpClient) {
    String engine = properties.engine() == null ? "meili" : properties.engine().trim().toLowerCase();
    return switch (engine) {
      case "opensearch" -> new OpenSearchDocumentSearchIndex(properties, objectMapper, searchHttpClient);
      case "meili", "meilisearch" -> new MeiliDocumentSearchIndex(properties, objectMapper, searchHttpClient);
      default -> throw new IllegalArgumentException("Unsupported search engine: " + properties.engine());
    };
  }
}
