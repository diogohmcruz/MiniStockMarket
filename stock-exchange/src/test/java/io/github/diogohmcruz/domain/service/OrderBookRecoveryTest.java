package io.github.diogohmcruz.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

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

/** The books are in memory only: after losing them, they must be rebuilt from the database with the same priority. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({OrderMatchingService.class, OrderBookService.class, TradeService.class})
class OrderBookRecoveryTest {

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
    OrderBookService book;

    @Autowired
    TradeRepository trades;

    private static Order sell(String ticker, String price, int quantity, String user, long sequence) {
        var o = new Order();
        o.setType(OrderType.SELL);
        o.setTicker(ticker);
        o.setPrice(new BigDecimal(price));
        o.setQuantity(quantity);
        o.setUserId(user);
        o.setSequence(sequence);
        o.setExpirationTime(Instant.now().plusSeconds(3600));
        return o;
    }

    private static Order buy(String ticker, String price, int quantity, String user) {
        var o = sell(ticker, price, quantity, user, 100);
        o.setType(OrderType.BUY);
        return o;
    }

    @Test
    void a_rebuilt_book_keeps_remainders_and_priority_and_skips_closed_orders() {
        matching.match(sell("RCA", "100.00", 10, "s-first", 1));
        matching.match(sell("RCA", "100.00", 10, "s-second", 2));
        var cancelled = sell("RCA", "99.00", 10, "s-cancelled", 3);
        matching.match(cancelled);
        book.cancelOrder(cancelled.getId(), "s-cancelled");
        matching.match(buy("RCA", "100.00", 4, "b1")); // s-first now has 6 left

        book.dropBooks(instrument -> true); // simulate a restart: the books are gone
        var loaded = book.rebuildBooks(instrument -> instrument.equals("MAIN:RCA"));
        assertThat(loaded).isEqualTo(2);

        matching.match(buy("RCA", "100.00", 8, "b2")); // 6 from s-first, then 2 from s-second

        var after = trades.findAll().stream()
                .filter(t -> t.getTicker().equals("RCA"))
                .sorted(java.util.Comparator.comparing(Trade::getTimestamp))
                .toList();
        assertThat(after).extracting(Trade::getSellerId).containsExactly("s-first", "s-first", "s-second");
        assertThat(after).extracting(Trade::getQuantity).containsExactly(4, 6, 2);
    }

    @Test
    void rebuilding_only_replaces_the_requested_tickers() {
        matching.match(sell("RCB", "50.00", 5, "keep", 1));
        matching.match(sell("RCC", "50.00", 5, "reload", 1));

        book.dropBooks(instrument -> instrument.equals("MAIN:RCC"));
        matching.match(buy("RCB", "50.00", 5, "b1"));
        matching.match(buy("RCC", "50.00", 5, "b2")); // no book: rests instead of trading

        assertThat(trades.findAll().stream().filter(t -> t.getTicker().equals("RCB")))
                .hasSize(1);
        assertThat(trades.findAll().stream().filter(t -> t.getTicker().equals("RCC")))
                .isEmpty();

        book.rebuildBooks(instrument -> instrument.equals("MAIN:RCC"));
        matching.match(buy("RCC", "50.00", 5, "b3")); // matches the reloaded sell
        assertThat(trades.findAll().stream().filter(t -> t.getTicker().equals("RCC")))
                .hasSize(1);
    }
}
