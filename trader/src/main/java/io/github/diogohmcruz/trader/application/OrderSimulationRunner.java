package io.github.diogohmcruz.trader.application;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.diogohmcruz.marketlibrary.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.marketlibrary.api.dto.OrderResponse;
import io.github.diogohmcruz.trader.infrastructure.generator.RandomOrderGenerator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
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

    private static void logResponse(OrderResponse orderResponse) {
        log.info("User {} sent order {}", orderResponse.getUserId(), orderResponse);
    }

    @PostConstruct
    private void sendRequests() {
        IntStream.range(0, executor.getMaxPoolSize()).forEach(i -> scheduleNext());
    }

    private void scheduleNext() {
        CompletableFuture.supplyAsync(orderGenerator::generateOrderRequest, executor)
                .thenApplyAsync(
                        createOrderRequest -> this.sendRequest(createOrderRequest)
                                .thenAccept(OrderSimulationRunner::logResponse)
                                .exceptionally(ex -> handleException(ex, createOrderRequest)),
                        executor)
                .exceptionally(throwable -> {
                    log.warn(throwable.getMessage(), throwable);
                    return null;
                })
                .thenRunAsync(this::scheduleNext, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS, executor));
    }

    @Async("traderTaskExecutor")
    public CompletableFuture<OrderResponse> sendRequest(CreateOrderRequest order) {
        var userId = Thread.currentThread().getName();
        return webClient
                .post()
                .uri("/api/orders")
                .header("user", userId)
                .bodyValue(order)
                .retrieve()
                .bodyToMono(OrderResponse.class)
                .toFuture();
    }
}
