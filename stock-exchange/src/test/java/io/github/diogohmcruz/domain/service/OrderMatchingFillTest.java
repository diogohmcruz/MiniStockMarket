package io.github.diogohmcruz.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import io.github.diogohmcruz.stockexchange.domain.repositories.OrderRepository;
import io.github.diogohmcruz.stockexchange.domain.repositories.TradeRepository;
import io.github.diogohmcruz.stockexchange.domain.service.OrderBookService;
import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;
import io.github.diogohmcruz.stockexchange.domain.service.TradeService;
import io.github.diogohmcruz.stockexchange.infrastructure.messaging.OrderPublisher;
import io.github.diogohmcruz.stockexchange.infrastructure.messaging.TradePublisher;

/**
 * Real matching, book and persistence on an in-memory database; only Kafka is mocked. Each test uses its own ticker
 * because the book is shared by the Spring context. Not wrapped in a transaction, like production.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({OrderMatchingService.class, OrderBookService.class, TradeService.class})
class OrderMatchingFillTest {

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = Order.class)
    @EnableJpaRepositories(basePackageClasses = OrderRepository.class)
    static class TestApp {}

    @MockitoBean
    OrderPublisher orderPublisher;

    @MockitoBean
    TradePublisher tradePublisher;

    @Autowired
    OrderMatchingService matching;

    @Autowired
    TradeRepository trades;

    @Autowired
    OrderRepository orders;

    private static Order order(OrderType type, String ticker, String price, int quantity, String user) {
        var o = new Order();
        o.setType(type);
        o.setTicker(ticker);
        o.setPrice(new BigDecimal(price));
        o.setQuantity(quantity);
        o.setUserId(user);
        o.setExpirationTime(Instant.now().plusSeconds(3600));
        return o;
    }

    private List<Trade> tradesOf(String ticker) {
        return trades.findAll().stream()
                .filter(t -> t.getTicker().equals(ticker))
                .sorted(java.util.Comparator.comparing(Trade::getTimestamp))
                .toList();
    }

    @Test
    void aResting_order_is_filled_partially_and_keeps_the_rest() {
        matching.match(order(OrderType.SELL, "FILA", "100.00", 10, "seller"));
        matching.match(order(OrderType.BUY, "FILA", "101.00", 4, "buyer-1"));

        var t = tradesOf("FILA");
        assertThat(t).hasSize(1);
        assertThat(t.get(0).getQuantity()).isEqualTo(4);
        assertThat(t.get(0).getPrice()).isEqualByComparingTo("100.00");

        var sell = orders.findAll().stream()
                .filter(o -> o.getTicker().equals("FILA") && o.getType() == OrderType.SELL)
                .findFirst()
                .orElseThrow();
        assertThat(sell.getFilledQuantity()).isEqualTo(4);
        assertThat(sell.getRemainingQuantity()).isEqualTo(6);
        assertThat(sell.isActive()).isTrue();
    }

    @Test
    void a_big_order_walks_the_book_and_prices_move() {
        matching.match(order(OrderType.SELL, "FILB", "100.00", 5, "s1"));
        matching.match(order(OrderType.SELL, "FILB", "102.00", 5, "s2"));

        matching.match(order(OrderType.BUY, "FILB", "105.00", 8, "b1")); // 5 @ 100, then 3 @ 102
        matching.match(order(OrderType.BUY, "FILB", "105.00", 5, "b2")); // 2 @ 102, 3 rest

        var t = tradesOf("FILB");
        assertThat(t).extracting(x -> x.getPrice().intValue()).containsExactly(100, 102, 102);
        assertThat(t).extracting(Trade::getQuantity).containsExactly(5, 3, 2);
        assertThat(t).extracting(Trade::getSellerId).containsExactly("s1", "s2", "s2");

        var b2 = orders.findAll().stream()
                .filter(o -> o.getUserId().equals("b2") && o.getTicker().equals("FILB"))
                .findFirst()
                .orElseThrow();
        assertThat(b2.getRemainingQuantity()).isEqualTo(3);
        assertThat(b2.isActive()).isTrue();
    }

    @Test
    void a_fully_filled_order_never_trades_again() {
        matching.match(order(OrderType.SELL, "FILC", "100.00", 5, "seller"));
        matching.match(order(OrderType.BUY, "FILC", "100.00", 5, "b1"));
        matching.match(order(OrderType.BUY, "FILC", "100.00", 5, "b2"));

        assertThat(tradesOf("FILC")).hasSize(1);
        var seller = orders.findAll().stream()
                .filter(o -> o.getUserId().equals("seller") && o.getTicker().equals("FILC"))
                .findFirst()
                .orElseThrow();
        assertThat(seller.isActive()).isFalse();
        assertThat(seller.getRemainingQuantity()).isZero();
    }

    @Test
    void orders_that_do_not_cross_just_rest() {
        matching.match(order(OrderType.SELL, "FILD", "110.00", 5, "seller"));
        matching.match(order(OrderType.BUY, "FILD", "100.00", 5, "buyer"));

        assertThat(tradesOf("FILD")).isEmpty();
    }

    @Test
    void sell_side_incoming_orders_match_the_best_bid() {
        matching.match(order(OrderType.BUY, "FILE", "99.00", 5, "b-low"));
        matching.match(order(OrderType.BUY, "FILE", "103.00", 5, "b-high"));
        matching.match(order(OrderType.SELL, "FILE", "98.00", 7, "seller")); // 5 @ 103, 2 @ 99

        var t = tradesOf("FILE");
        assertThat(t).extracting(x -> x.getPrice().intValue()).containsExactly(103, 99);
        assertThat(t).extracting(Trade::getQuantity).containsExactly(5, 2);
        assertThat(t).extracting(Trade::getBuyerId).containsExactly("b-high", "b-low");
    }
}
