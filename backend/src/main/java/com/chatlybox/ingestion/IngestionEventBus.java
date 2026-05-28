package com.chatlybox.ingestion;

import java.time.Instant;
import java.util.UUID;

import com.chatlybox.ingestion.dto.IngestionEvent;
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

}
