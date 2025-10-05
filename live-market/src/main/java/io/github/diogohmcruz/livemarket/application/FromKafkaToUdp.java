package io.github.diogohmcruz.livemarket.application;

import org.springframework.stereotype.Component;

import io.github.diogohmcruz.livemarket.service.TickerStatsCache;
import io.github.diogohmcruz.livemarket.service.TradeListener;
import lombok.RequiredArgsConstructor;

/**
 * Orchestrates the ingestion pipeline: Kafka -> in-memory stats -> UDP broadcast. This component primarily documents
 * and exposes minimal operational hooks over the reactive/event-driven pieces (the actual consumption is performed via
 * {@link TradeListener}).
 */
@Component
@RequiredArgsConstructor
public class FromKafkaToUdp {

    private final TickerStatsCache cache;

    /** Simple health indicator for the stats cache (could be expanded later). */
    public boolean isCacheWarm(String ticker) {
        return cache.getTicker(ticker) != null;
    }
}
