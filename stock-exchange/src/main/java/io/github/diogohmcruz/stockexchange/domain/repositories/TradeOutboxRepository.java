package io.github.diogohmcruz.stockexchange.domain.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.diogohmcruz.stockexchange.domain.model.TradeOutboxEvent;

public interface TradeOutboxRepository extends JpaRepository<TradeOutboxEvent, UUID> {
    long countByAggregateId(String aggregateId);
}
