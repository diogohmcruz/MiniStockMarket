package io.github.diogohmcruz.stockexchange.domain.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.diogohmcruz.stockexchange.domain.model.OrderOutboxEvent;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEvent, UUID> {
    List<OrderOutboxEvent> findAllByOrderIdOrderByCreatedAt(UUID orderId);
}
