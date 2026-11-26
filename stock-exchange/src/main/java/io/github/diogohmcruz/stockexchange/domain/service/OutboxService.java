package io.github.diogohmcruz.stockexchange.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderEventDTO;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderEventType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.OrderOutboxEvent;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import io.github.diogohmcruz.stockexchange.domain.model.TradeOutboxEvent;
import io.github.diogohmcruz.stockexchange.domain.repositories.OrderOutboxRepository;
import io.github.diogohmcruz.stockexchange.domain.repositories.TradeOutboxRepository;
import io.github.diogohmcruz.stockexchange.infrastructure.messaging.Mappers;
import lombok.RequiredArgsConstructor;

/**
 * Transactional outbox: events are inserted in the same transaction as the state change they describe (hence
 * {@code MANDATORY}), so an event exists if and only if the change committed. Publishing to Kafka is done outside the
 * application by change data capture on these tables.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OrderOutboxRepository orderOutbox;
    private final TradeOutboxRepository tradeOutbox;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void orderEvent(OrderEventType type, Order order) {
        record(type, order, null);
    }

    /** Records the fill of one side of a trade; the order must already have been filled. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void fill(Order order, Trade trade) {
        record(OrderEventType.FILL, order, trade);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void trade(Trade trade) {
        var dto = Mappers.toDto(trade);
        tradeOutbox.save(new TradeOutboxEvent(instrumentOf(trade), json(dto), trade.getId()));
    }

    private void record(OrderEventType type, Order order, Trade trade) {
        var event = new OrderEventDTO();
        event.setType(type);
        event.setOrder(Mappers.toDto(order));
        if (trade != null) {
            event.setTradeId(trade.getId());
            event.setQuantity(trade.getQuantity());
            event.setPrice(trade.getPrice());
        }
        orderOutbox.save(new OrderOutboxEvent(
                order.getInstrument(), type, json(event), order.getId(), trade == null ? null : trade.getId()));
    }

    private static String instrumentOf(Trade trade) {
        return io.github.diogohmcruz.marketlibrary.domain.model.Instrument.key(trade.getVenue(), trade.getTicker());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise outbox payload", e);
        }
    }
}
