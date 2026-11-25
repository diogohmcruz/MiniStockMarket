package io.github.diogohmcruz.stockexchange.domain.service;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Periodically marks orders past their expiration time as EXPIRED, keeping any shares already executed. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "order.expiration.enabled", havingValue = "true", matchIfMissing = true)
class OrderExpirationJob {
    private final OrderBookService orderBookService;

    @Scheduled(fixedDelayString = "${order.expiration.sweep-interval-ms:5000}")
    void sweep() {
        try {
            var expired = orderBookService.expireOrders(Instant.now());
            if (expired > 0) {
                log.info("Expired {} orders", expired);
            }
        } catch (RuntimeException e) {
            // Typically an optimistic lock conflict with the matcher; the next sweep retries.
            log.warn("Order expiration sweep failed: {}", e.getMessage());
        }
    }
}
