package io.github.diogohmcruz.domain.service;

import io.github.diogohmcruz.domain.model.Order;
import io.github.diogohmcruz.domain.model.OrderType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderBookService {

  @PersistenceContext
  private EntityManager entityManager;

  private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
  private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

  @Transactional
  public void addOrder(Order order) {
    entityManager.persist(order);
    getOrCreateOrderBook(order.getTicker()).addOrder(order);
    log.info("New order added: {} {}x {} at {}",
        order.getType(),
        order.getQuantity(),
        order.getTicker(),
        order.getPrice());
  }

  @Transactional(readOnly = true)
  public Optional<Order> getOrderById(UUID orderId) {
    return Optional.ofNullable(entityManager.find(Order.class, orderId));
  }

  @Transactional
  public Order getBestMatchingOrder(Order order) {
    String ticker = order.getTicker();
    OrderBook orderBook = orderBooks.get(ticker);
    if (orderBook == null) {
      return null;
    }

    Order matchingOrder = orderBook.getBestMatchingOrder(order);
    if (matchingOrder != null) {
      try {
        // Try to find and lock the order in the database
        Order freshOrder = entityManager.find(
            Order.class,
            matchingOrder.getId(),
            LockModeType.PESSIMISTIC_WRITE
        );

        // If order exists and is still valid
        if (freshOrder != null && freshOrder.isValidForMatching()) {
          return freshOrder;
        }

        // If order is invalid or doesn't exist, remove it from order book
        orderBook.removeOrder(matchingOrder);
        // Try to find next best match
        return getBestMatchingOrder(order);

      } catch (Exception e) {
        log.warn("Failed to refresh order {}: {}", matchingOrder.getId(), e.getMessage());
        orderBook.removeOrder(matchingOrder);
        return getBestMatchingOrder(order);
      }
    }
    return null;
  }

    @Transactional(readOnly = true)
    public List<Order> getActiveBuyOrders(String ticker) {
      OrderBook orderBook = orderBooks.get(ticker);
      if (orderBook == null) {
          return Collections.emptyList();
      }

      List<Order> orders = orderBook.getActiveBuyOrders();
      List<Order> validOrders = new ArrayList<>();

      for (Order order : orders) {
        try {
          Order freshOrder = entityManager.find(Order.class, order.getId());
          if (freshOrder != null && freshOrder.isValidForMatching()) {
            validOrders.add(freshOrder);
          } else {
            orderBook.removeOrder(order);
          }
        } catch (Exception e) {
          log.warn("Failed to refresh buy order {}: {}", order.getId(), e.getMessage());
          orderBook.removeOrder(order);
        }
      }

      return validOrders;
    }

    @Transactional(readOnly = true)
    public List<Order> getActiveSellOrders(String ticker) {
        OrderBook orderBook = orderBooks.get(ticker);
        if (orderBook == null) {
            return Collections.emptyList();
        }

        List<Order> orders = orderBook.getActiveSellOrders();
        List<Order> validOrders = new ArrayList<>();

        for (Order order : orders) {
            try {
                Order freshOrder = entityManager.find(Order.class, order.getId());
                if (freshOrder != null && freshOrder.isValidForMatching()) {
                    validOrders.add(freshOrder);
                } else {
                    orderBook.removeOrder(order);
                }
            } catch (Exception e) {
                log.warn("Failed to refresh sell order {}: {}", order.getId(), e.getMessage());
                orderBook.removeOrder(order);
            }
        }

        return validOrders;
    }

    @Transactional
    public boolean cancelOrder(UUID orderId, String userId) {
        Order order = entityManager.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
        if (order == null || !order.isActive() || !order.getUserId().equals(userId)) {
            return false;
        }

        order.setActive(false);
        OrderBook orderBook = orderBooks.get(order.getTicker());
        if (orderBook != null) {
            orderBook.removeOrder(order);
        }

        log.info("Order cancelled: {} by user {}", orderId, userId);
        return true;
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(String userId, boolean includeInactive, Pageable pageable) {
        String jpql = "SELECT o FROM Order o WHERE o.userId = :userId " +
                     (includeInactive ? "" : "AND o.active = true ") +
                     "ORDER BY o.timestamp DESC";

        var query = entityManager.createQuery(jpql, Order.class)
            .setParameter("userId", userId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize());

        var countQuery = entityManager.createQuery(
            "SELECT COUNT(o) FROM Order o WHERE o.userId = :userId " +
            (includeInactive ? "" : "AND o.active = true "),
            Long.class)
            .setParameter("userId", userId);

        var orders = query.getResultList();
        var total = countQuery.getSingleResult();

        return new org.springframework.data.domain.PageImpl<>(orders, pageable, total);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderBookStatistics(Instant since) {
        Map<String, Object> stats = new HashMap<>();

        String orderCountsJpql = """
            SELECT o.ticker, o.type, COUNT(o)
            FROM Order o
            WHERE o.active = true
            GROUP BY o.ticker, o.type
        """;

        var orderCounts = entityManager.createQuery(orderCountsJpql, Object[].class)
            .getResultList()
            .stream()
            .collect(Collectors.groupingBy(
                row -> (String) row[0],
                Collectors.toMap(
                    row -> (OrderType) row[1],
                    row -> (Long) row[2]
                )
            ));

        String priceRangesJpql = """
            SELECT o.ticker,
                   MIN(CASE WHEN o.type = 'SELL' THEN o.price END),
                   MAX(CASE WHEN o.type = 'BUY' THEN o.price END)
            FROM Order o
            WHERE o.active = true
            GROUP BY o.ticker
        """;

        var priceRanges = entityManager.createQuery(priceRangesJpql, Object[].class)
            .getResultList()
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> Map.of(
                    "lowestAsk", row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
                    "highestBid", row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO
                )
            ));

        stats.put("orderCounts", orderCounts);
        stats.put("priceRanges", priceRanges);

        return stats;
    }

    private OrderBook getOrCreateOrderBook(String ticker) {
        OrderBook orderBook = orderBooks.get(ticker);
        if (orderBook == null) {
            globalLock.writeLock().lock();
            try {
                orderBook = orderBooks.computeIfAbsent(ticker, k -> new OrderBook());
            } finally {
                globalLock.writeLock().unlock();
            }
        }
        return orderBook;
    }

    private static class OrderBook {
      private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<>(
        11,
        Comparator.<Order>comparingDouble(o -> o.getPrice().doubleValue())
          .reversed()
          .thenComparing(Order::getTimestamp)
      );

      private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<>(
        11,
        Comparator.<Order>comparingDouble(o -> o.getPrice().doubleValue())
            .thenComparing(Order::getTimestamp)
      );

      private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void addOrder(Order order) {
      lock.writeLock().lock();
      try {
        if (order.getType() == OrderType.BUY) {
          buyOrders.offer(order);
        } else {
          sellOrders.offer(order);
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    public Order getBestMatchingOrder(Order order) {
            lock.readLock().lock();
      try {
        PriorityBlockingQueue<Order> matchingOrders = OrderType.BUY.equals(order.getType())
                ? sellOrders
                : buyOrders;

        return matchingOrders.peek();
      } finally {
                lock.readLock().unlock();
      }
    }
        public void removeOrder(Order order) {
            lock.writeLock().lock();
            try {
                if (order.getType() == OrderType.BUY) {
                    buyOrders.remove(order);
                } else {
                    sellOrders.remove(order);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public List<Order> getActiveBuyOrders() {
            lock.readLock().lock();
            try {
                return new ArrayList<>(buyOrders);
            } finally {
                lock.readLock().unlock();
            }
        }

        public List<Order> getActiveSellOrders() {
            lock.readLock().lock();
            try {
                return new ArrayList<>(sellOrders);
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
