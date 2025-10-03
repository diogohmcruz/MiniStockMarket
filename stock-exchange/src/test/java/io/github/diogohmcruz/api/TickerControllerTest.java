package io.github.diogohmcruz.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.StockExchangeApplication;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.repositories.OrderRepository;
import io.github.diogohmcruz.stockexchange.domain.service.TickerService;

@SpringBootTest(classes = StockExchangeApplication.class)
class TickerControllerTest {
    @Autowired
    private TickerService tickerService;

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    @Test
    void getAllTickers_ShouldReturnListOfTickers() {
        var tickers = Stream.of("AAPL", "GOOGL", "MSFT")
                .map(ticker -> {
                    var order = new Order();
                    order.setUserId("test");
                    order.setType(OrderType.BUY);
                    order.setTicker(ticker);
                    order.setPrice(BigDecimal.TEN);
                    order.setQuantity(10);
                    order.setExpirationTime(Instant.now().plusSeconds(60));
                    order.setActive(true);
                    return order;
                })
                .map(o -> orderRepository.save(o))
                .toList();
        var activeTicker = tickerService.getAllActiveTickers();
        assertThat(activeTicker).hasSameSizeAs(tickers);
    }
}
