package io.github.diogohmcruz.livemarket.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;

class TradeListenerTest {

    private TickerStatsCache cache;
    private TickerAnalyst analyst;
    private TestBroadcaster broadcaster;
    private TradeListener listener;

    @BeforeEach
    void setUp() {
        cache = new TickerStatsCache();
        analyst = new TickerAnalyst(cache);
        broadcaster = new TestBroadcaster(new ObjectMapper());
        listener = new TradeListener(analyst, broadcaster);
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
        assertThat(broadcaster.sentCount).isEqualTo(1);
        assertThat(cache.getTicker("NFLX").getLastPrice()).isEqualByComparingTo("402.11");
    }

    @Test
    void onTradeIgnoresNullEvent() {
        listener.onTrade(null);
        assertThat(broadcaster.sentCount).isZero();
    }

    static class TestBroadcaster extends UdpBroadcaster {
        int sentCount = 0;

        public TestBroadcaster(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        public void send(Object payload) {
            // Do not call super to avoid netty init; just record
            sentCount++;
        }
    }
}
