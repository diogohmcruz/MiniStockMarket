package io.github.diogohmcruz.stockexchange.application;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;

@Component
public class OrderSimulationRunner {

  private final OrderMatchingService orderMatchingService;
  private final ThreadPoolTaskExecutor executor;

  public OrderSimulationRunner(
      OrderMatchingService orderMatchingService,
      @Qualifier("brokerTaskExecutor") ThreadPoolTaskExecutor executor) {
    this.orderMatchingService = orderMatchingService;
    this.executor = executor;
  }

  @PostConstruct
  public void processOrders() {
    for (int i = 0; i < executor.getMaxPoolSize(); i++) {
      scheduleNext(orderMatchingService);
    }
  }

  private void scheduleNext(OrderMatchingService service) {
    CompletableFuture.runAsync(service::processOrders, executor)
        .thenRun(() -> scheduleNext(service));
  }

  @PreDestroy
  public void onShutdown() {
    executor.shutdown();
  }
}
