package io.github.diogohmcruz.livemarket.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;

class TickerAnalystTest {

    private TickerStatsCache cache;
    private TickerAnalyst analyst;

    @BeforeEach
    void setUp() {
        cache = new TickerStatsCache();
        analyst = new TickerAnalyst(cache);
    }

    @Test
    void processReturnsEmptyForInvalidEvent() {
        var invalid = new TradeDTO();
        invalid.setId(UUID.randomUUID());
        invalid.setTicker("AAPL");
        invalid.setPrice(BigDecimal.TEN);
        invalid.setQuantity(0);
        invalid.setTimestamp(Instant.now());
        assertThat(analyst.process(invalid)).isNull();
    }

    @Test
    void processUpdatesCacheAndReturnsTickerInfo() {
        var trade = new TradeDTO();
        trade.setId(UUID.randomUUID());
        trade.setTicker("MSFT");
        trade.setPrice(new BigDecimal("150.25"));
        trade.setQuantity(50);
        trade.setBuyerId("buyer");
        trade.setSellerId("seller");
        trade.setTimestamp(Instant.now());
        var info = analyst.process(trade);
        assertThat(info).isNotNull();
        assertThat(info.getSymbol()).isEqualTo("MSFT");
        assertThat(info.getLastPrice()).isEqualByComparingTo("150.25");
        assertThat(info.getLastTradeTime()).isNotNull();
        var snapshot = cache.getTicker("MSFT");
        assertThat(snapshot.getLastPrice()).isEqualByComparingTo("150.25");
    }
}
