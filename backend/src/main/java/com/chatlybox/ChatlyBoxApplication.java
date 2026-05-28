package com.chatlybox;

import com.chatlybox.ingestion.IngestionProperties;
import com.chatlybox.search.SearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({IngestionProperties.class, SearchProperties.class})
public class ChatlyBoxApplication {
  public static void main(String[] args) {
    SpringApplication.run(ChatlyBoxApplication.class, args);
  }
}
