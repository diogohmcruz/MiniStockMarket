package io.github.diogohmcruz.stockexchange.domain.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import io.github.diogohmcruz.stockexchange.infrastructure.messaging.OrderPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order matching engine logic. This service no longer owns any submission queue; it only takes already-submitted orders
 * (arriving via messaging) and attempts to match them against the existing order book, producing trades when possible.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderMatchingService {
    private final OrderBookService orderBookService;
    private final OrderPublisher orderPublisher;
    private final TradeService tradeService;

    /**
     * Process a single incoming order attempting to match it.
     *
     * <p>If it can be matched with an existing opposite-side order a trade is executed;
     *
     * <p>Otherwise, it is added to the order book.
     *
     * @param order domain order reconstructed from an OrderEvent
     * @return id of the processed order
     */
    public UUID match(Order order) {
        if (!order.isValidForMatching()) {
            var errorMessage = String.format("Rejected invalid order from %s: expired or inactive", order.getUserId());
            throw new IllegalArgumentException(errorMessage);
        }

        var matchingOrder = orderBookService.getBestMatchingOrder(order);
        if (canMatch(order, matchingOrder)) {
            executeTrade(order, matchingOrder);
        } else {
            orderBookService.addOrder(order);
        }
        return order.getId();
    }

    public CompletableFuture<OrderDTO> submit(Order order) {
        return orderPublisher
                .publish(order)
                .thenApply(SendResult::getProducerRecord)
                .thenApply(ProducerRecord::value);
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

        return tradeService.processTrade(trade);
    }
}
