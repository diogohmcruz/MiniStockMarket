package io.github.diogohmcruz.stockexchange.domain.repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Ticker;
import io.github.diogohmcruz.stockexchange.domain.model.TickerCandle;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAllByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<Order> findAllByUserIdAndStatusInOrderByTimestampDesc(
            String userId, Collection<OrderStatus> statuses, Pageable pageable);

    List<Order> findAllByStatusInAndExpirationTimeBefore(Collection<OrderStatus> statuses, Instant now);

    List<Order> findAllByStatusIn(Collection<OrderStatus> statuses);

    @Query(
            """
          SELECT o.ticker AS ticker, o.type AS type, COUNT(o) AS count
          FROM Order o
          WHERE o.status IN (io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus.OPEN, io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus.PARTIALLY_FILLED)
          AND o.timestamp >= :since
          GROUP BY o.ticker, o.type
      """)
    List<Ticker> findAllTickerCounts(@Param("since") Instant since);

    @Query(
            """
          SELECT o.ticker AS ticker,
                 MIN(CASE WHEN o.type = 'SELL' THEN o.price END) AS lowestAsk,
                 MAX(CASE WHEN o.type = 'BUY' THEN o.price END) AS highestBid
          FROM Order o
          WHERE o.status IN (io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus.OPEN, io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus.PARTIALLY_FILLED)
          AND o.timestamp >= :since
          GROUP BY o.ticker
      """)
    List<TickerCandle> findAllPriceRanges(@Param("since") Instant since);
}
