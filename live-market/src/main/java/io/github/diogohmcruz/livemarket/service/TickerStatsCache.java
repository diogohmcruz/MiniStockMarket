package io.github.diogohmcruz.livemarket.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.TickerInfo;

/** Caches ticker statistics for quick access and retrieval. */
@Component
public class TickerStatsCache {
    private final Map<String, TickerInfo> statsByTicker = new ConcurrentHashMap<>();

    public TickerInfo getTicker(String ticker) {
        return statsByTicker.computeIfAbsent(ticker, _ -> new TickerInfo());
    }

    public void saveTicker(TickerInfo updatedInfo) {
        statsByTicker.put(updatedInfo.getSymbol(), updatedInfo);
    }

    public List<TickerInfo> listAll() {
        return List.copyOf(statsByTicker.values());
    }
}
