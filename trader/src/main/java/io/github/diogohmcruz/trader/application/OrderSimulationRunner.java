package io.github.diogohmcruz.trader.application;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.diogohmcruz.marketlibrary.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.trader.infrastructure.generator.RandomOrderGenerator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true", matchIfMissing = true)
public class OrderSimulationRunner {

    private final RandomOrderGenerator orderGenerator;
    private final WebClient webClient;
    private final ThreadPoolTaskExecutor executor;

    public OrderSimulationRunner(
            RandomOrderGenerator orderGenerator,
            WebClient webClient,
            @Qualifier("traderTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.orderGenerator = orderGenerator;
        this.webClient = webClient;
        this.executor = executor;
    }

    private static Void handleException(Throwable ex, CreateOrderRequest order) {
        var cause = ex.getCause();
        if (cause instanceof WebClientException) {
            var isResponse = cause instanceof WebClientResponseException;
            var causeMessage = isResponse
                    ? ((WebClientResponseException) cause).getResponseBodyAsString()
                    : ((WebClientRequestException) cause).getClass().getName();
            log.error("Failed to send order: {} with cause: {}", order, causeMessage);
        } else {
            log.error("Failed to send order: {}", order, cause);
        }
        return null;
    }

    private static void logResponse(OrderDTO orderDTO) {
        log.info("User {} sent order {}", orderDTO.getUserId(), orderDTO);
    }

    @PostConstruct
    private void sendRequests() {
        IntStream.range(0, executor.getMaxPoolSize()).forEach(i -> scheduleNext());
    }

    public void scheduleNext() {
        var orderRequest = orderGenerator.generateOrderRequest();
        this.sendRequest(orderRequest)
                .thenAccept(OrderSimulationRunner::logResponse)
                .exceptionally(ex -> handleException(ex, orderRequest))
                .thenRunAsync(this::scheduleNext, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS, executor));
    }

    @Async("traderTaskExecutor")
    public CompletableFuture<OrderDTO> sendRequest(CreateOrderRequest order) {
        var userId = Thread.currentThread().getName();
        return webClient
                .post()
                .uri("/api/orders")
                .header("user", userId)
                .bodyValue(order)
                .retrieve()
                .bodyToMono(OrderDTO.class)
                .toFuture();
    }
}
