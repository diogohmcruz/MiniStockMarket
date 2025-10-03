package io.github.diogohmcruz.stockexchange.domain.service;

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

    private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<>(
            11,
            Comparator.<Order>comparingDouble(o -> o.getPrice().doubleValue())
                    .reversed()
                    .thenComparing(Order::getTimestamp));

    private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<>(
            11,
            Comparator.<Order>comparingDouble(o -> o.getPrice().doubleValue()).thenComparing(Order::getTimestamp));

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
