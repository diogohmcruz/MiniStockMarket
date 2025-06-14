package io.github.diogohmcruz.trader.application;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.diogohmcruz.marketlibrary.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.marketlibrary.api.dto.OrderResponse;
import io.github.diogohmcruz.trader.infrastructure.config.TraderThreadFactory;
import io.github.diogohmcruz.trader.infrastructure.generator.RandomOrderGenerator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderSimulationRunner {
  private final WebClient webClient;
  private static final int NUM_THREADS = 10;
  private final Object lock = new Object();

  public OrderSimulationRunner(RandomOrderGenerator orderGenerator, WebClient webClient) {
    this.webClient = webClient;
    var traderThreadFactory = TraderThreadFactory.builder().nameFormat("trader-%d").build();
    try (ExecutorService executor =
        Executors.newFixedThreadPool(NUM_THREADS, traderThreadFactory)) {
      for (int i = 0; i < NUM_THREADS; i++) {
        executor.submit(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                try {
                  CreateOrderRequest order = orderGenerator.generateOrderRequest();
                  sendRequest(order)
                      .thenAccept(OrderSimulationRunner::logResponse)
                      .exceptionally(ex -> handleException(ex, order));

                  synchronized (lock) {
                    lock.wait(500);
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                } catch (Exception e) {
                  log.error("Unexpected error in order simulation", e);
                }
              }
            });
      }
    }
  }

  private static Void handleException(Throwable ex, CreateOrderRequest order) {
    log.error("Failed to send order: {}", order, ex);
    return null;
  }

  private static void logResponse(OrderResponse orderResponse) {
    log.info("User {} sent order {}", orderResponse.getUserId(), orderResponse);
  }

  @Async
  public CompletableFuture<OrderResponse> sendRequest(CreateOrderRequest order) {
    String userId = Thread.currentThread().getName();
    return webClient
        .post()
        .uri("/api/orders")
        .header("user", userId)
        .bodyValue(order)
        .retrieve()
        .bodyToMono(OrderResponse.class)
        .toFuture();
  }
}
