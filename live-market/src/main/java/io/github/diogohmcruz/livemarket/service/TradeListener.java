package io.github.diogohmcruz.livemarket.service;

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

    @KafkaListener(topics = "${live-market.kafka.trade-topic}")
    public void onTrade(@Payload TradeDTO trade) {
        var ticker = analyst.process(trade);
        log.info("{} -> {}", ticker.getSymbol(), ticker);
        broadcaster.send(ticker);
    }
}
