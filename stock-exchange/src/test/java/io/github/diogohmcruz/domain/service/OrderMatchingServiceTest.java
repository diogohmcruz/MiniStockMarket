package io.github.diogohmcruz.domain.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.StockExchangeApplication;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = StockExchangeApplication.class)
public class OrderMatchingServiceTest {
  @Autowired private OrderMatchingService orderMatchingService;

  @Test
  void testConcurrentOrderProcessing() throws InterruptedException {
    try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
      int numberOfOrders = 100;

      // Submit buy and sell orders concurrently
      for (int i = 0; i < numberOfOrders; i++) {
        final int orderNumber = i;
        executorService.submit(
            () -> {
              Order order = new Order();
              order.setType(orderNumber % 2 == 0 ? OrderType.BUY : OrderType.SELL);
              order.setTicker("AAPL");
              order.setPrice(new BigDecimal("100.00"));
              order.setQuantity(10);
              order.setUserId("test-" + orderNumber);
              orderMatchingService.submitOrder(order);
            });
      }

      executorService.shutdown();
      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
  }
}
