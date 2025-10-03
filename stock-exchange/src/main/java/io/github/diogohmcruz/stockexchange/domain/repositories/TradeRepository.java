package io.github.diogohmcruz.stockexchange.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.diogohmcruz.stockexchange.domain.model.Trade;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    List<Trade> findByTickerOrderByTimestampDesc(String ticker);

    Page<Trade> findByBuyerIdOrSellerIdOrderByTimestampDesc(String buyerId, String sellerId, Pageable pageable);

    List<Trade> findByBuyerIdOrSellerIdOrderByTimestampDesc(String buyerId, String sellerId);

    @Query("SELECT t FROM Trade t LEFT JOIN FETCH t.buyOrder LEFT JOIN FETCH t.sellOrder WHERE t.id =" + " :id")
    Optional<Trade> findByIdWithOrders(@Param("id") UUID id);

    @Query("SELECT t FROM Trade t WHERE t.ticker = :ticker " + "ORDER BY t.timestamp DESC")
    List<Trade> findRecentTradesByTicker(@Param("ticker") String ticker);

    @Query("SELECT t FROM Trade t "
            + "WHERE (t.buyerId = :userId OR t.sellerId = :userId) "
            + "AND t.timestamp BETWEEN :startDate AND :endDate "
            + "ORDER BY t.timestamp DESC")
    List<Trade> findTradesByUserInDateRange(
            @Param("userId") String userId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.ticker = :ticker AND t.timestamp >= :since")
    long countRecentTrades(@Param("ticker") String ticker, @Param("since") Instant since);

    List<Trade> findAllByTimestampGreaterThanOrderByTimestampDesc(Instant timestamp);

    List<Trade> findAllByTimestampGreaterThanEqual(Instant timestamp);

    @Query("SELECT t FROM Trade t WHERE t.buyerId = :userId OR t.sellerId = :userId " + "ORDER BY t.timestamp DESC")
    List<Trade> findUserTrades(@Param("userId") String userId);
}
