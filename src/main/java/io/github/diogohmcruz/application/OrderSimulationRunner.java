package io.github.diogohmcruz.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.domain.model.Order;
import io.github.diogohmcruz.domain.service.OrderMatchingService;
import io.github.diogohmcruz.infrastructure.generator.RandomOrderGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSimulationRunner {
  private final OrderMatchingService orderMatchingService;
  private final RandomOrderGenerator orderGenerator;

  @Scheduled(fixedRate = 100) // Generate order every second
  public void generateOrder() {
    Order order = orderGenerator.generateOrder();
    orderMatchingService.submitOrder(order);
  }

  @Scheduled(fixedRate = 1000) // Process orders every 100ms
  public void processOrders() {
    orderMatchingService.processOrders();
  }
}
