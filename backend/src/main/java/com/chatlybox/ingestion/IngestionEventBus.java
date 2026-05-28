package com.chatlybox.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class IngestionEventBus {
  private final Sinks.Many<IngestionEvent> sink =
      Sinks.many().multicast().onBackpressureBuffer(1024, false);

  public void publish(IngestionEvent event) {
    sink.tryEmitNext(event);
  }

  public Flux<IngestionEvent> stream() {
    return sink.asFlux();
  }

  public record IngestionEvent(
      UUID sourceId,
      String stage,
      String message,
      int documents,
      int chunks,
      Instant timestamp) {
    public static IngestionEvent of(UUID sourceId, String stage, String message, int documents, int chunks) {
      return new IngestionEvent(sourceId, stage, message, documents, chunks, Instant.now());
    }
  }
}
