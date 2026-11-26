package io.github.diogohmcruz.stockexchange.domain.service;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderEventType;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import io.github.diogohmcruz.stockexchange.infrastructure.TransactionCallbacks;
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
    private final OutboxService outboxService;

    /**
     * Process a single incoming order attempting to match it.
     *
     * <p>Everything that must be consistent (the incoming order, every partially or fully filled resting order and the
     * trades) is written in one transaction, on managed entities, so a failure leaves the database untouched. The
     * in-memory book is only changed once the outcome is known: resting orders that were taken out of the book while
     * matching are put back on rollback, and the remainders are (re)inserted after commit.
     *
     * <p>Delivery is at-least-once, so an order whose id was already processed is ignored.
     *
     * @param order domain order reconstructed from an OrderEvent
     * @return id of the processed order
     */
    @Transactional
    public UUID match(Order order) {
        if (!order.isValidForMatching()) {
            var errorMessage = String.format("Rejected invalid order from %s: expired or inactive", order.getUserId());
            throw new IllegalArgumentException(errorMessage);
        }
        if (orderBookService.exists(order.getId())) {
            log.warn("Ignoring redelivered order {}", order.getId());
            return order.getId();
        }

        var incoming = orderBookService.persist(order);
        outboxService.orderEvent(OrderEventType.NEW, incoming);
        var takenFromBook = new ArrayList<Order>();
        var traded = new ArrayList<Order>();
        TransactionCallbacks.afterRollback(() -> takenFromBook.forEach(orderBookService::addToBook));

        while (incoming.getRemainingQuantity() > 0) {
            var candidate = orderBookService.peekBest(incoming);
            if (candidate == null) {
                break;
            }
            var resting = orderBookService.getOrderById(candidate.getId()).orElse(null);
            if (resting == null || !resting.isValidForMatching()) {
                // Stale index entry (filled, cancelled or expired meanwhile): drop it and keep looking.
                if (resting != null && resting.expire()) {
                    outboxService.orderEvent(OrderEventType.EXPIRED, resting);
                }
                orderBookService.removeFromBook(candidate);
                takenFromBook.add(candidate);
                continue;
            }
            if (!crosses(incoming, resting)) {
                break;
            }
            orderBookService.removeFromBook(candidate);
            takenFromBook.add(candidate);
            executeTrade(incoming, resting);
            traded.add(resting);
        }

        TransactionCallbacks.afterCommit(() -> {
            traded.stream().filter(Order::isValidForMatching).forEach(orderBookService::addToBook);
            if (incoming.isValidForMatching()) {
                orderBookService.addToBook(incoming);
            }
        });
        return incoming.getId();
    }

    public CompletableFuture<OrderDTO> submit(Order order) {
        return orderPublisher
                .publish(order)
                .thenApply(SendResult::getProducerRecord)
                .thenApply(ProducerRecord::value);
    }

    private boolean crosses(Order incoming, Order resting) {
        return incoming.getType() == OrderType.BUY
                ? incoming.getPrice().compareTo(resting.getPrice()) >= 0
                : resting.getPrice().compareTo(incoming.getPrice()) >= 0;
    }

    private void executeTrade(Order incoming, Order resting) {
        var incomingIsBuy = incoming.getType() == OrderType.BUY;
        var buyOrder = incomingIsBuy ? incoming : resting;
        var sellOrder = incomingIsBuy ? resting : incoming;
        var trade = new Trade(buyOrder, sellOrder, resting.getPrice());
        buyOrder.fill(trade.getQuantity());
        sellOrder.fill(trade.getQuantity());
        var saved = tradeService.processTrade(trade);
        outboxService.fill(buyOrder, saved);
        outboxService.fill(sellOrder, saved);
    }
}
