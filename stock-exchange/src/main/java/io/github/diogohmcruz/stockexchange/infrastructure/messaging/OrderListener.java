package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
@ConditionalOnProperty(name = "live-market.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OrderListener {

    private final OrderMatchingService matchingService;

    @KafkaListener(topics = "${live-market.kafka.order-topic}")
    public void onOrder(@Payload OrderDTO orderDTO, @Header(KafkaHeaders.OFFSET) long offset) {
        var order = map(orderDTO);
        order.setSequence(offset);
        matchingService.match(order);
    }

    private static Order map(OrderDTO orderDTO) {
        var order = new Order();
        if (orderDTO.getId() != null) {
            order.setId(orderDTO.getId());
        }
        order.setTicker(orderDTO.getTicker());
        order.setTimestamp(orderDTO.getTimestamp());
        order.setUserId(orderDTO.getUserId());
        order.setType(orderDTO.getType());
        order.setPrice(orderDTO.getPrice());
        order.setQuantity(orderDTO.getQuantity());
        order.setExpirationTime(orderDTO.getExpirationTime());
        return order;
    }
}
