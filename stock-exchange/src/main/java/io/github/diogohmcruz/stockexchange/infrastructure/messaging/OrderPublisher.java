package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import java.util.UUID;
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
    private final KafkaTemplate<UUID, OrderDTO> kafkaTemplate;

    @Value("${live-market.kafka.order-topic}")
    private String orderTopic;

    @Async
    public CompletableFuture<SendResult<UUID, OrderDTO>> publish(Order order) {
        var orderDTO = map(order);
        return kafkaTemplate.send(orderTopic, order.getId(), orderDTO).whenComplete((_, ex) -> {
            if (ex != null) {
                log.error("Async failure publishing order event {}: {}", order.getId(), ex.getMessage());
            } else {
                log.debug("Published order event {} to topic {}", order.getId(), orderTopic);
            }
        });
    }

    private static OrderDTO map(Order order) {
        var orderDTO = new OrderDTO();
        orderDTO.setTicker(order.getTicker());
        orderDTO.setTimestamp(order.getTimestamp());
        orderDTO.setActive(order.isActive());
        orderDTO.setUserId(order.getUserId());
        orderDTO.setType(order.getType());
        orderDTO.setPrice(order.getPrice());
        orderDTO.setQuantity(order.getQuantity());
        orderDTO.setExpirationTime(order.getExpirationTime());
        return orderDTO;
    }
}
