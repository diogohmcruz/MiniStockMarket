package io.github.diogohmcruz.stockexchange.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.diogohmcruz.marketlibrary.domain.model.Instrument;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.repositories.OrderRepository;
import io.github.diogohmcruz.stockexchange.infrastructure.TransactionCallbacks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Owns the in-memory order books and the persistence of orders. The books are only an index of resting orders (price
 * and arrival priority); the authoritative state of an order is always the database row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {
    private final OrderRepository orderRepository;

    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public boolean exists(UUID orderId) {
        return orderRepository.existsById(orderId);
    }

    /** Persists a brand new order and returns the managed instance. */
    @Transactional
    public Order persist(Order order) {
        return orderRepository.save(order);
    }

    /** Best resting order on the opposite side, as indexed by the book. Does not touch the database. */
    public Order peekBest(Order incoming) {
        var orderBook = orderBooks.get(incoming.getInstrument());
        return orderBook == null ? null : orderBook.getBestMatchingOrder(incoming);
    }

    public void addToBook(Order order) {
        var added = getOrCreateOrderBook(order.getInstrument()).addOrder(order);
        if (added) {
            log.info(
                    "Order resting in book: {} {}x {} at {} ({} remaining)",
                    order.getType(),
                    order.getQuantity(),
                    order.getTicker(),
                    order.getPrice(),
                    order.getRemainingQuantity());
        }
    }

    public void removeFromBook(Order order) {
        var orderBook = orderBooks.get(order.getInstrument());
        if (orderBook != null) {
            orderBook.removeOrder(order);
        }
    }

    /**
     * Rebuilds the books of the instruments accepted by the filter from the database. The books live in memory only, so
     * this runs whenever this instance is handed the partitions of those instruments (startup, rebalance). Each order
     * keeps its price/sequence priority because both are persisted. Overdue orders are kept in the book so that the
     * next expiration sweep finds and expires them; matching skips them as stale.
     *
     * @return number of resting orders loaded
     */
    @Transactional(readOnly = true)
    public int rebuildBooks(Predicate<String> instrumentFilter) {
        var resting = orderRepository.findAllByStatusIn(OrderStatus.ACTIVE).stream()
                .filter(order -> instrumentFilter.test(order.getInstrument()))
                .collect(Collectors.groupingBy(Order::getInstrument));
        orderBooks.keySet().removeIf(instrumentFilter);
        resting.forEach((instrument, orders) -> {
            var orderBook = new OrderBook(instrument);
            orders.forEach(orderBook::addOrder);
            orderBooks.put(instrument, orderBook);
        });
        return resting.values().stream().mapToInt(List::size).sum();
    }

    /** Forgets the books of instruments this instance no longer owns. */
    public void dropBooks(Predicate<String> instrumentFilter) {
        orderBooks.keySet().removeIf(instrumentFilter);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> getActiveBuyOrders(String venue, String ticker) {
        var orderBook = orderBooks.get(Instrument.key(venue, ticker));
        if (orderBook == null) {
            return Collections.emptyList();
        }
        return refresh(orderBook, orderBook.getActiveBuyOrders());
    }

    @Transactional(readOnly = true)
    public List<Order> getActiveSellOrders(String venue, String ticker) {
        var orderBook = orderBooks.get(Instrument.key(venue, ticker));
        if (orderBook == null) {
            return Collections.emptyList();
        }
        return refresh(orderBook, orderBook.getActiveSellOrders());
    }

    /** Fresh view of the indexed orders. Stale entries are dropped from the book only when the database says so. */
    private List<Order> refresh(OrderBook orderBook, List<Order> indexed) {
        var validOrders = new ArrayList<Order>();
        indexed.forEach(order -> orderRepository
                .findById(order.getId())
                .ifPresentOrElse(
                        fresh -> {
                            if (fresh.isValidForMatching()) {
                                validOrders.add(fresh);
                            } else {
                                orderBook.removeOrder(order);
                            }
                        },
                        () -> orderBook.removeOrder(order)));
        return validOrders;
    }

    @Transactional
    public boolean cancelOrder(UUID orderId, String userId) {
        var order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(userId) || !order.cancel()) {
            return false;
        }
        TransactionCallbacks.afterCommit(() -> removeFromBook(order));
        log.info("Order cancelled: {} by user {}", orderId, userId);
        return true;
    }

    /**
     * Marks every active order past its expiration time as EXPIRED, keeping what was already executed. Only the
     * instruments whose book this instance holds are swept: those are the ones whose partitions it owns, so the expiry,
     * its outbox event and the removal from the book happen on the owner and several instances never compete for the
     * same orders.
     */
    @Transactional
    public int expireOrders(Instant now) {
        var ownedInstruments = List.copyOf(orderBooks.keySet());
        if (ownedInstruments.isEmpty()) {
            return 0;
        }
        var candidates = orderRepository.findAllOverdueByInstrumentIn(OrderStatus.ACTIVE, now, ownedInstruments);
        var expired = 0;
        for (var order : candidates) {
            if (order.expire()) {
                expired++;
                TransactionCallbacks.afterCommit(() -> removeFromBook(order));
            }
        }
        return expired;
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(String userId, boolean includeInactive, Pageable pageable) {
        return includeInactive
                ? orderRepository.findAllByUserIdOrderByTimestampDesc(userId, pageable)
                : orderRepository.findAllByUserIdAndStatusInOrderByTimestampDesc(userId, OrderStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderBookStatistics(Instant since) {
        var tickers = orderRepository.findAllTickerCounts(since);
        var tickerCandles = orderRepository.findAllPriceRanges(since);

        return Map.of("orderCounts", tickers, "priceRanges", tickerCandles);
    }

    private OrderBook getOrCreateOrderBook(String instrument) {
        return orderBooks.computeIfAbsent(instrument, OrderBook::new);
    }
}
