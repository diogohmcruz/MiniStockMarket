package io.github.diogohmcruz.stockexchange.domain.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.diogohmcruz.stockexchange.domain.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {}
