package io.github.diogohmcruz.domain.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.diogohmcruz.domain.model.Order;
import io.github.diogohmcruz.domain.model.OrderType;

@SpringBootTest
public class OrderMatchingServiceTest {
  @Autowired private OrderMatchingService orderMatchingService;

  @Test
  void testConcurrentOrderProcessing() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    int numberOfOrders = 100;

    // Submit buy and sell orders concurrently
    for (int i = 0; i < numberOfOrders; i++) {
      final int orderNumber = i;
      executorService.submit(
          () -> {
            Order order =
                new Order(
                    orderNumber % 2 == 0 ? OrderType.BUY : OrderType.SELL,
                    "AAPL",
                    new BigDecimal("100.00"),
                    10,
                    "user-" + orderNumber);
            orderMatchingService.submitOrder(order);
          });
    }

    executorService.shutdown();
    assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
  }
}
