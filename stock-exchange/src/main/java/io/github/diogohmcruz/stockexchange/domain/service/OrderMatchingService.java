package io.github.diogohmcruz.stockexchange.domain.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Service;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderMatchingService {
  private final OrderBookService orderBookService;
  private final TradeService tradeService;
  private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();

  public void submitOrder(Order order) {
    if (order.isValidForMatching()) {
      orderQueue.offer(order);
    } else {
      log.warn("Rejected invalid order {}: expired or inactive", order.getId());
    }
  }

  public void processOrders() {
    try {
      Order order = orderQueue.take();
      if (!order.isValidForMatching()) {
        log.debug("Skipping invalid order {}: expired or inactive", order.getId());
        return;
      }

      Order matchingOrder = orderBookService.getBestMatchingOrder(order);
      if (canMatch(order, matchingOrder)) {
        executeTrade(order, matchingOrder);
      } else {
        orderBookService.addOrder(order);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Order processing interrupted", e);
    }
  }

  private boolean canMatch(Order order1, Order order2) {
    if (order2 == null || !order2.isValidForMatching()) {
      return false;
    }

    return order1.getType() == OrderType.BUY
        ? order1.getPrice().compareTo(order2.getPrice()) >= 0
        : order2.getPrice().compareTo(order1.getPrice()) >= 0;
  }

  private Trade executeTrade(Order order1, Order order2) {
    Order buyOrder = order1.getType() == OrderType.BUY ? order1 : order2;
    Order sellOrder = order1.getType() == OrderType.SELL ? order1 : order2;
    Trade trade = new Trade(buyOrder, sellOrder, order2.getPrice());

    return tradeService.saveTrade(trade);
  }
}
