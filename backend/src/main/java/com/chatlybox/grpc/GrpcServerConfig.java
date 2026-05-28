package com.chatlybox.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerConfig implements SmartLifecycle {
  private final Server server;
  private volatile boolean running;

  public GrpcServerConfig(@Value("${chatly.grpc.port}") int port, RagGrpcService ragGrpcService) {
    this.server = NettyServerBuilder.forPort(port).addService(ragGrpcService).build();
  }

  @Override
  public void start() {
    try {
      server.start();
      running = true;
    } catch (IOException error) {
      throw new IllegalStateException("Failed to start gRPC server", error);
    }
  }

  @Override
  public void stop() {
    server.shutdown();
    running = false;
  }

  @PreDestroy
  void destroy() {
    stop();
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
