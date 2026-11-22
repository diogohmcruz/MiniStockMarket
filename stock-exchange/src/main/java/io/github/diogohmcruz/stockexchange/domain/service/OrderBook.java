package io.github.diogohmcruz.stockexchange.domain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import lombok.Getter;
import lombok.Locked;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class OrderBook {
    @Getter
    private final String ticker;

    /**
     * Time priority among orders at the same price: the arrival sequence (Kafka offset within the ticker partition)
     * wins; orders without a sequence fall back to their creation timestamp.
     */
    private static final Comparator<Order> TIME_PRIORITY = Comparator.comparing(
                    Order::getSequence, Comparator.nullsLast(Comparator.<Long>naturalOrder()))
            .thenComparing(Order::getTimestamp);

    private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<>(
            11, Comparator.comparing(Order::getPrice, Comparator.<BigDecimal>reverseOrder()).thenComparing(TIME_PRIORITY));

    private final PriorityBlockingQueue<Order> sellOrders =
            new PriorityBlockingQueue<>(11, Comparator.comparing(Order::getPrice).thenComparing(TIME_PRIORITY));

    @Locked.Write
    public boolean addOrder(Order order) {
        return order.getType() == OrderType.BUY ? buyOrders.offer(order) : sellOrders.offer(order);
    }

    @Locked.Read
    public Order getBestMatchingOrder(Order order) {
        var matchingOrders = OrderType.BUY.equals(order.getType()) ? sellOrders : buyOrders;
        return matchingOrders.peek();
    }

    @Locked.Write
    public boolean removeOrder(Order order) {
        return order.getType() == OrderType.BUY ? buyOrders.remove(order) : sellOrders.remove(order);
    }

    @Locked.Read
    public List<Order> getActiveBuyOrders() {
        return new ArrayList<>(buyOrders);
    }

    @Locked.Read
    public List<Order> getActiveSellOrders() {
        return new ArrayList<>(sellOrders);
    }
}
