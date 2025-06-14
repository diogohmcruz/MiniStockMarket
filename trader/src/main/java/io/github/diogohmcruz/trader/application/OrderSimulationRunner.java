package io.github.diogohmcruz.trader.application;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.diogohmcruz.marketlibrary.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.marketlibrary.api.dto.OrderResponse;
import io.github.diogohmcruz.trader.infrastructure.generator.RandomOrderGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSimulationRunner {

  private final RandomOrderGenerator orderGenerator;
  private final WebClient webClient;

  @Scheduled(fixedRate = 100)
  public void generateOrder() {
    CreateOrderRequest order = orderGenerator.generateOrderRequest();
    String userId = "user-" + UUID.randomUUID().toString().substring(0, 5);

    try {
      sendRequest(userId, order)
          .thenAccept(response -> log.info("Order sent successfully: {}", order))
          .exceptionally(
              ex -> {
                log.error("Failed to send order: {}", order, ex);
                return null;
              });
    } catch (Exception e) {
      log.error("Unexpected failure: {}", order, e);
    }
  }

  @Async
  CompletableFuture<OrderResponse> sendRequest(String userId, CreateOrderRequest order) {
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
