package io.github.diogohmcruz.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus;
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

/** Order lifecycle guarantees: idempotency, final fills, priority and transactional consistency of book and events. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({OrderMatchingService.class, OrderBookService.class, TradeService.class})
class OrderMatchingLifecycleTest {

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = Order.class)
    @EnableJpaRepositories(basePackageClasses = OrderRepository.class)
    static class TestApp {}

    @MockitoBean
    OrderPublisher orderPublisher;

    @MockitoBean
    TradePublisher tradePublisher;

    @MockitoSpyBean
    TradeService tradeService;

    @Autowired
    OrderMatchingService matching;

    @Autowired
    OrderBookService book;

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

    private static Order order(OrderType type, String ticker, String price, int quantity, String user, long sequence) {
        var o = order(type, ticker, price, quantity, user);
        o.setSequence(sequence);
        return o;
    }

    private long tradeCount(String ticker) {
        return trades.findAll().stream().filter(t -> t.getTicker().equals(ticker)).count();
    }

    @Test
    void a_redelivered_order_is_processed_only_once() {
        matching.match(order(OrderType.SELL, "LCA", "100.00", 5, "seller"));
        var buy = order(OrderType.BUY, "LCA", "100.00", 5, "buyer");
        var copy = order(OrderType.BUY, "LCA", "100.00", 5, "buyer");
        copy.setId(buy.getId());

        matching.match(buy);
        matching.match(copy);

        assertThat(tradeCount("LCA")).isEqualTo(1);
        assertThat(orders.findById(buy.getId())).get().extracting(Order::getStatus).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void executed_shares_are_final_when_an_order_expires() {
        var sell = order(OrderType.SELL, "LCB", "100.00", 10, "seller");
        matching.match(sell);
        matching.match(order(OrderType.BUY, "LCB", "100.00", 4, "b1"));

        var expired = book.expireOrders(Instant.now().plusSeconds(7200));
        assertThat(expired).isPositive();

        var stored = orders.findById(sell.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(stored.getFilledQuantity()).isEqualTo(4);

        matching.match(order(OrderType.BUY, "LCB", "100.00", 6, "b2"));
        assertThat(tradeCount("LCB")).isEqualTo(1);
    }

    @Test
    void cancelling_a_partially_filled_order_keeps_what_was_executed() {
        var sell = order(OrderType.SELL, "LCC", "100.00", 10, "seller");
        matching.match(sell);
        matching.match(order(OrderType.BUY, "LCC", "100.00", 3, "b1"));

        assertThat(book.cancelOrder(sell.getId(), "seller")).isTrue();

        var stored = orders.findById(sell.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stored.getFilledQuantity()).isEqualTo(3);

        matching.match(order(OrderType.BUY, "LCC", "100.00", 7, "b2"));
        assertThat(tradeCount("LCC")).isEqualTo(1);
    }

    @Test
    void at_the_same_price_the_earlier_arrival_trades_first() {
        matching.match(order(OrderType.SELL, "LCD", "100.00", 5, "late", 9));
        matching.match(order(OrderType.SELL, "LCD", "100.00", 5, "early", 3));

        matching.match(order(OrderType.BUY, "LCD", "100.00", 5, "buyer"));

        assertThat(trades.findAll().stream().filter(t -> t.getTicker().equals("LCD")))
                .extracting(Trade::getSellerId)
                .containsExactly("early");
    }

    @Test
    void a_failed_match_rolls_everything_back_and_puts_the_book_back() {
        var sell = order(OrderType.SELL, "LCE", "100.00", 5, "seller");
        matching.match(sell);

        doThrow(new IllegalStateException("boom")).doCallRealMethod().when(tradeService).processTrade(any());
        var failing = order(OrderType.BUY, "LCE", "100.00", 5, "b1");
        assertThatThrownBy(() -> matching.match(failing)).isInstanceOf(IllegalStateException.class);

        assertThat(orders.findById(failing.getId())).isEmpty();
        assertThat(orders.findById(sell.getId())).get().extracting(Order::getFilledQuantity).isEqualTo(0);
        verify(tradePublisher, never()).publish(any());

        // The resting order is back in the book and can still be matched.
        matching.match(order(OrderType.BUY, "LCE", "100.00", 5, "b2"));
        assertThat(tradeCount("LCE")).isEqualTo(1);
        verify(tradePublisher, times(1)).publish(any());
    }

    @Test
    void unknown_orders_cannot_be_cancelled() {
        assertThat(book.cancelOrder(UUID.randomUUID(), "nobody")).isFalse();
    }
}
