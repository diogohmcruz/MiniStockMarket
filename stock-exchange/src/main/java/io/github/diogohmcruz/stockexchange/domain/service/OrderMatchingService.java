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

        var incoming = orderBookService.save(order);
        var restingOrder = orderBookService.getBestMatchingOrder(incoming);
        while (incoming.getRemainingQuantity() > 0 && canMatch(incoming, restingOrder)) {
            incoming = executeTrade(incoming, restingOrder);
            restingOrder = orderBookService.getBestMatchingOrder(incoming);
        }
        if (incoming.getRemainingQuantity() > 0) {
            orderBookService.addOrder(incoming);
        }
        return incoming.getId();
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

    private Order executeTrade(Order incoming, Order resting) {
        var incomingIsBuy = incoming.getType() == OrderType.BUY;
        var buyOrder = incomingIsBuy ? incoming : resting;
        var sellOrder = incomingIsBuy ? resting : incoming;
        var trade = new Trade(buyOrder, sellOrder, resting.getPrice());
        buyOrder.fill(trade.getQuantity());
        sellOrder.fill(trade.getQuantity());

        var saved = tradeService.processTrade(trade);
        return incomingIsBuy ? saved.getBuyOrder() : saved.getSellOrder();
    }
}
