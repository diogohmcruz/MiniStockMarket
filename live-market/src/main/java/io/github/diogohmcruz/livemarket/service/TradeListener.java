package io.github.diogohmcruz.livemarket.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeListener {

    private final TickerAnalyst analyst;
    private final UdpBroadcaster broadcaster;

    private static final int MAX_TRACKED_TRADES = 10_000;

    private final Set<UUID> seenTrades = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
            return size() > MAX_TRACKED_TRADES;
        }
    }));

    @KafkaListener(topics = "${live-market.kafka.trade-topic}")
    public void onTrade(@Payload TradeDTO trade) {
        if (trade == null || (trade.getId() != null && !seenTrades.add(trade.getId()))) {
            return;
        }
        var ticker = analyst.process(trade);
        log.info("{} -> {}", ticker.getSymbol(), ticker);
        broadcaster.send(ticker);
    }
}
