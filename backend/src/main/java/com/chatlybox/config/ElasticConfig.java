package com.chatlybox.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticConfig {
  @Bean
  RestClient elasticRestClient(@Value("${spring.elasticsearch.uris}") String uris) {
    String firstUri = uris.split(",")[0].trim();
    return RestClient.builder(HttpHost.create(firstUri)).build();
  }

  @Bean
  ElasticsearchClient elasticsearchClient(RestClient restClient) {
    return new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
  }
}
