package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradePublisher {

    private final KafkaTemplate<String, TradeDTO> kafkaTemplate;

    @Value("${live-market.kafka.trade-topic}")
    private String tradeTopic;

    @Async
    public CompletableFuture<SendResult<String, TradeDTO>> publish(Trade trade) {
        var tradeDTO = map(trade);
        return kafkaTemplate
                .send(tradeTopic, trade.getId().toString(), tradeDTO)
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        log.error("Async failure publishing trade event {}: {}", trade.getId(), ex.getMessage());
                    } else {
                        log.debug("Published trade event {} to topic {}", trade.getId(), tradeTopic);
                    }
                });
    }

    private static TradeDTO map(Trade trade) {
        var tradeDTO = new TradeDTO();
        tradeDTO.setId(trade.getId());
        tradeDTO.setVenue(trade.getVenue());
        tradeDTO.setTicker(trade.getTicker());
        tradeDTO.setTimestamp(trade.getTimestamp());
        tradeDTO.setBuyOrderId(trade.getBuyOrder().getId());
        tradeDTO.setSellOrderId(trade.getSellOrder().getId());
        tradeDTO.setBuyerId(trade.getBuyerId());
        tradeDTO.setSellerId(trade.getSellerId());
        tradeDTO.setPrice(trade.getPrice());
        tradeDTO.setQuantity(trade.getQuantity());
        tradeDTO.setVersion(trade.getVersion());
        return tradeDTO;
    }
}
