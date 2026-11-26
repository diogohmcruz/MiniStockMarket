package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPublisher {
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Value("${live-market.kafka.order-topic}")
    private String orderTopic;

    @Async
    public CompletableFuture<SendResult<String, OrderDTO>> publish(Order order) {
        var orderDTO = Mappers.toDto(order);
        return kafkaTemplate.send(orderTopic, order.getInstrument(), orderDTO).whenComplete((_, ex) -> {
            if (ex != null) {
                log.error("Async failure publishing order event {}: {}", order.getId(), ex.getMessage());
            } else {
                log.debug("Published order event {} to topic {}", order.getId(), orderTopic);
            }
        });
    }
}
