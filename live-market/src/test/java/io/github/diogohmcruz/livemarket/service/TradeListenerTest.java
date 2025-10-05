package io.github.diogohmcruz.livemarket.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;

class TradeListenerTest {

    private TickerStatsCache cache;
    private TickerAnalyst analyst;
    private TradeListener listener;

    @BeforeEach
    void setUp() {
        cache = new TickerStatsCache();
        analyst = new TickerAnalyst(cache);
        listener = new TradeListener(analyst);
        // Manually enable broadcast flag via reflection since @Value isn't processed here
        try {
            var f = TradeListener.class.getDeclaredField("broadcastEnabled");
            f.setAccessible(true);
            f.setBoolean(listener, true);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void onTradeBroadcastsTickerInfo() {
        var trade = new TradeDTO();
        trade.setId(UUID.randomUUID());
        trade.setTicker("NFLX");
        trade.setPrice(new BigDecimal("402.11"));
        trade.setQuantity(10);
        trade.setBuyerId("b1");
        trade.setSellerId("s1");
        trade.setTimestamp(Instant.now());
        listener.onTrade(trade);
        assertThat(cache.getTicker("NFLX").getLastPrice()).isEqualByComparingTo("402.11");
    }
}
