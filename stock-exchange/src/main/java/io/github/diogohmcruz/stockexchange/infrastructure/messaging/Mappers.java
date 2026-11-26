package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;

/** Conversions between the domain model and the DTOs that cross the Kafka and REST boundaries. */
public final class Mappers {

    private Mappers() {}

    public static OrderDTO toDto(Order order) {
        var dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setType(order.getType());
        dto.setVenue(order.getVenue());
        dto.setTicker(order.getTicker());
        dto.setPrice(order.getPrice());
        dto.setQuantity(order.getQuantity());
        dto.setFilledQuantity(order.getFilledQuantity());
        dto.setTimestamp(order.getTimestamp());
        dto.setExpirationTime(order.getExpirationTime());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setActive(order.isActive());
        return dto;
    }

    /** Builds a new order from its intake message; the status always starts at OPEN. */
    public static Order toNewOrder(OrderDTO dto) {
        var order = new Order();
        if (dto.getId() != null) {
            order.setId(dto.getId());
        }
        if (dto.getVenue() != null) {
            order.setVenue(dto.getVenue());
        }
        order.setTicker(dto.getTicker());
        order.setTimestamp(dto.getTimestamp());
        order.setUserId(dto.getUserId());
        order.setType(dto.getType());
        order.setPrice(dto.getPrice());
        order.setQuantity(dto.getQuantity());
        order.setExpirationTime(dto.getExpirationTime());
        return order;
    }

    public static TradeDTO toDto(Trade trade) {
        var dto = new TradeDTO();
        dto.setId(trade.getId());
        dto.setVenue(trade.getVenue());
        dto.setTicker(trade.getTicker());
        dto.setTimestamp(trade.getTimestamp());
        dto.setBuyOrderId(trade.getBuyOrder().getId());
        dto.setSellOrderId(trade.getSellOrder().getId());
        dto.setBuyerId(trade.getBuyerId());
        dto.setSellerId(trade.getSellerId());
        dto.setPrice(trade.getPrice());
        dto.setQuantity(trade.getQuantity());
        dto.setVersion(trade.getVersion());
        return dto;
    }
}
