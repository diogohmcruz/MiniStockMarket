package io.github.diogohmcruz.stockexchange.application;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;

@Component
public class OrderSimulationRunner {

    private final OrderMatchingService orderMatchingService;
    private final ThreadPoolTaskExecutor executor;

    public OrderSimulationRunner(
            OrderMatchingService orderMatchingService,
            @Qualifier("brokerTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.orderMatchingService = orderMatchingService;
        this.executor = executor;
    }

    @PostConstruct
    public void processOrders() {
        IntStream.range(0, executor.getMaxPoolSize()).forEach(i -> this.scheduleNext());
    }

    private void scheduleNext() {
        CompletableFuture.supplyAsync(this::processOrder, executor).thenRunAsync(this::scheduleNext);
    }

    private UUID processOrder() {
        try {
            return orderMatchingService.processOrders();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
