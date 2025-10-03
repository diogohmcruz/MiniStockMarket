package io.github.diogohmcruz.stockexchange.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TickerService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<String> getAllActiveTickers() {
        // Get unique tickers from active orders
        var tickers = new TreeSet<>(getActiveOrderTickers());

        // Add tickers from recent trades (last 24 hours)
        tickers.addAll(getRecentTradeTickers());

        log.info("Retrieved {} active tickers", tickers.size());
        return tickers.stream().toList();
    }

    private List<String> getActiveOrderTickers() {
        return entityManager
                .createQuery(
                        "SELECT DISTINCT o.ticker FROM Order o "
                                + "WHERE o.active = true AND o.expirationTime > :now "
                                + "ORDER BY o.ticker",
                        String.class)
                .setParameter("now", Instant.now())
                .getResultList();
    }

    private List<String> getRecentTradeTickers() {
        return entityManager
                .createQuery(
                        "SELECT DISTINCT t.ticker FROM Trade t " + "WHERE t.timestamp > :dayAgo " + "ORDER BY t.ticker",
                        String.class)
                .setParameter("dayAgo", Instant.now().minusSeconds(86400))
                .getResultList();
    }
}
