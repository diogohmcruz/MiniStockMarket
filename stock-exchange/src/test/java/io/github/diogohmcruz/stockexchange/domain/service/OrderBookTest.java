package io.github.diogohmcruz.stockexchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;

class OrderBookTest {

    private final OrderBook book = new OrderBook("AAPL");

    @Test
    void sellSide_bestPriceFirst_thenLowestSequence() {
        var late = sell("10.00", 7L);
        var early = sell("10.00", 3L);
        var cheaper = sell("9.99", 99L);
        book.addOrder(late);
        book.addOrder(early);
        book.addOrder(cheaper);

        var incomingBuy = order(OrderType.BUY, "20.00", null);
        assertThat(book.getBestMatchingOrder(incomingBuy)).isSameAs(cheaper);
        book.removeOrder(cheaper);
        assertThat(book.getBestMatchingOrder(incomingBuy)).isSameAs(early);
    }

    @Test
    void buySide_highestPriceFirst_thenLowestSequence() {
        var low = order(OrderType.BUY, "10.00", 1L);
        var highLate = order(OrderType.BUY, "11.00", 9L);
        var highEarly = order(OrderType.BUY, "11.00", 5L);
        book.addOrder(low);
        book.addOrder(highLate);
        book.addOrder(highEarly);

        var incomingSell = order(OrderType.SELL, "1.00", null);
        assertThat(book.getBestMatchingOrder(incomingSell)).isSameAs(highEarly);
    }

    @Test
    void priceComparisonIgnoresScale() {
        var a = sell("10.0", 2L);
        var b = sell("10.00", 1L);
        book.addOrder(a);
        book.addOrder(b);

        assertThat(book.getBestMatchingOrder(order(OrderType.BUY, "20", null))).isSameAs(b);
    }

    private static Order sell(String price, Long sequence) {
        return order(OrderType.SELL, price, sequence);
    }

    private static Order order(OrderType type, String price, Long sequence) {
        var order = new Order();
        order.setType(type);
        order.setTicker("AAPL");
        order.setPrice(new BigDecimal(price));
        order.setQuantity(10);
        order.setUserId("u");
        order.setExpirationTime(Instant.now().plusSeconds(60));
        order.setSequence(sequence);
        return order;
    }
}
