package io.github.diogohmcruz.stockexchange.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderSimulationRunner {
  private final OrderMatchingService orderMatchingService;

  @Scheduled(fixedRate = 1000)
  public void processOrders() {
    orderMatchingService.processOrders();
  }
}
