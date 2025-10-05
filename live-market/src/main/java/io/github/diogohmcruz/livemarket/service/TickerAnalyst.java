package io.github.diogohmcruz.livemarket.service;

import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.TickerInfo;
import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Calculates and analyzes ticker statistics from incoming market data. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TickerAnalyst {

    private final TickerStatsCache cache;

    /**
     * Process a trade event updating in-memory statistics.
     *
     * @param tradeDTO trade event
     * @return updated ticker info or empty if event ignored
     */
    public TickerInfo process(TradeDTO tradeDTO) {
        var ticker = cache.getTicker(tradeDTO.getTicker());
        var updatedInfo = updateTickerInfo(tradeDTO, ticker);
        cache.saveTicker(updatedInfo);
        return updatedInfo;
    }

    private TickerInfo updateTickerInfo(TradeDTO trade, TickerInfo tickerInfo) {
        var newInfo = new TickerInfo();
        newInfo.setSymbol(trade.getTicker());
        newInfo.setLastPrice(trade.getPrice());
        newInfo.setLastTradeTime(trade.getTimestamp());
        if (tickerInfo.getLastPrice() != null) {
            if (trade.getPrice().compareTo(tickerInfo.getLastPrice()) > 0) {
                newInfo.setActiveBuyOrders(trade.getQuantity());
            } else {
                newInfo.setActiveSellOrders(trade.getQuantity());
            }
        }
        return newInfo;
    }
}
