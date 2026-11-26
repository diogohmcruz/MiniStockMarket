package io.github.diogohmcruz.stockexchange.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderEventType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable record of something that happened to an order. Written in the same transaction as the change itself; the
 * change data capture connector (Debezium) turns every inserted row into a message on the {@code order-events} topic.
 * The column names follow Debezium's outbox event router.
 */
@Entity
@Table(name = "order_outbox")
@Getter
@NoArgsConstructor
public class OrderOutboxEvent {
    public static final String AGGREGATE_TYPE = "ORDER";

    @Id
    @Column(updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "aggregatetype", nullable = false, updatable = false)
    private String aggregateType = AGGREGATE_TYPE;

    /** Instrument key ({@code venue:ticker}); becomes the Kafka key so events of an instrument stay in order. */
    @Column(name = "aggregateid", nullable = false, updatable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrderEventType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "trade_id", updatable = false)
    private UUID tradeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OrderOutboxEvent(String aggregateId, OrderEventType type, String payload, UUID orderId, UUID tradeId) {
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.orderId = orderId;
        this.tradeId = tradeId;
    }
}
