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

    public boolean submitOrder(Order order) {
        if (!order.isValidForMatching()) {
            var errorMessage = String.format("Rejected invalid order from %s: expired or inactive", order.getUserId());
            throw new IllegalArgumentException(errorMessage);
        }
        return orderQueue.offer(order);
    }

    public void processOrders() {
        try {
            var order = orderQueue.take();
            if (!order.isValidForMatching()) {
                log.debug("Skipping invalid order {}: expired or inactive", order.getId());
                return;
            }

            var matchingOrder = orderBookService.getBestMatchingOrder(order);
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
        var buyOrder = order1.getType() == OrderType.BUY ? order1 : order2;
        var sellOrder = order1.getType() == OrderType.SELL ? order1 : order2;
        var trade = new Trade(buyOrder, sellOrder, order2.getPrice());

        return tradeService.saveTrade(trade);
    }
}
