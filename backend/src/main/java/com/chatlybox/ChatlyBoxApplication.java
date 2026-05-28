package com.chatlybox;

import com.chatlybox.ingestion.IngestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IngestionProperties.class)
public class ChatlyBoxApplication {
  public static void main(String[] args) {
    SpringApplication.run(ChatlyBoxApplication.class, args);
  }
}
