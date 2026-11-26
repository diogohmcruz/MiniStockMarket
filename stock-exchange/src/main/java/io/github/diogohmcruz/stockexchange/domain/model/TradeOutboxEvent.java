package io.github.diogohmcruz.stockexchange.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable record of an executed trade, written in the same transaction as the trade. Debezium publishes each row on
 * the {@code trades} topic.
 */
@Entity
@Table(name = "trade_outbox")
@Getter
@NoArgsConstructor
public class TradeOutboxEvent {
    public static final String AGGREGATE_TYPE = "TRADE";
    public static final String TYPE = "TRADE_EXECUTED";

    @Id
    @Column(updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "aggregatetype", nullable = false, updatable = false)
    private String aggregateType = AGGREGATE_TYPE;

    @Column(name = "aggregateid", nullable = false, updatable = false)
    private String aggregateId;

    @Column(nullable = false, updatable = false)
    private String type = TYPE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "trade_id", nullable = false, updatable = false)
    private UUID tradeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TradeOutboxEvent(String aggregateId, String payload, UUID tradeId) {
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.tradeId = tradeId;
    }
}
