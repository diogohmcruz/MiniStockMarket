package io.github.diogohmcruz.trader.application;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.diogohmcruz.marketlibrary.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.marketlibrary.api.dto.OrderResponse;
import io.github.diogohmcruz.trader.infrastructure.generator.RandomOrderGenerator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderSimulationRunner {

  private final RandomOrderGenerator orderGenerator;
  private final WebClient webClient;
  private final ThreadPoolTaskExecutor executor;
  private final Object lock = new Object();

  public OrderSimulationRunner(
      RandomOrderGenerator orderGenerator,
      WebClient webClient,
      @Qualifier("traderTaskExecutor") ThreadPoolTaskExecutor executor) {
    this.orderGenerator = orderGenerator;
    this.webClient = webClient;
    this.executor = executor;
  }

  private static Void handleException(Throwable ex, CreateOrderRequest order) {
    var cause = ex.getCause();
    if (cause instanceof WebClientException) {
      var isResponse = cause instanceof WebClientResponseException;
      var causeMessage =
          isResponse
              ? ((WebClientResponseException) cause).getResponseBodyAsString()
              : ((WebClientRequestException) cause).getClass().getName();
      log.error("Failed to send order: {} with cause: {}", order, causeMessage);
    } else {
      log.error("Failed to send order: {}", order, cause);
    }
    return null;
  }

  private static void logResponse(OrderResponse orderResponse) {
    log.info("User {} sent order {}", orderResponse.getUserId(), orderResponse);
  }

  @PostConstruct
  private void sendRequests() {
    for (int i = 0; i < executor.getMaxPoolSize(); i++) {
      scheduleNext();
    }
  }

  private void scheduleNext() {
    CompletableFuture.runAsync(
            () -> {
              try {
                CreateOrderRequest order = orderGenerator.generateOrderRequest();
                sendRequest(order)
                    .thenAccept(OrderSimulationRunner::logResponse)
                    .exceptionally(ex -> handleException(ex, order));

                synchronized (lock) {
                  lock.wait(50);
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (Exception e) {
                log.error("Unexpected error in order simulation", e);
              }
            },
            executor)
        .thenRun(this::scheduleNext);
  }

  @Async("traderTaskExecutor")
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

  @PreDestroy
  public void onShutdown() {
    executor.shutdown();
  }
}
