package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Consumes {@link Order} messages and delegates to the {@link OrderMatchingService}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

    private final OrderMatchingService matchingService;

    @KafkaListener(topics = "${live-market.kafka.order-topic}")
    public void onOrder(@Payload OrderDTO orderDTO) {
        var order = map(orderDTO);
        matchingService.match(order);
    }

    private static Order map(OrderDTO orderDTO) {
        var order = new Order();
        order.setTicker(orderDTO.getTicker());
        order.setTimestamp(orderDTO.getTimestamp());
        order.setActive(orderDTO.isActive());
        order.setUserId(orderDTO.getUserId());
        order.setType(orderDTO.getType());
        order.setPrice(orderDTO.getPrice());
        order.setQuantity(orderDTO.getQuantity());
        order.setExpirationTime(orderDTO.getExpirationTime());
        return order;
    }
}
