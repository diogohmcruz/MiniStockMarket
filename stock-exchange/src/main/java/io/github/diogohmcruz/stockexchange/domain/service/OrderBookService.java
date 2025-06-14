package io.github.diogohmcruz.stockexchange.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.repositories.OrderRepository;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {
  private final OrderRepository orderRepository;

  private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

  @Transactional
  public UUID addOrder(Order order) {
    var persistedOrder = orderRepository.save(order);
    var orderBook = getOrCreateOrderBook(persistedOrder.getTicker());
    var isAdded = orderBook.addOrder(persistedOrder);
    if (!isAdded) {
      return null;
    }
    log.info(
        "New order added: {} {}x {} at {}",
        persistedOrder.getType(),
        persistedOrder.getQuantity(),
        persistedOrder.getTicker(),
        persistedOrder.getPrice());
    return persistedOrder.getId();
  }

  @Transactional(readOnly = true)
  public Optional<Order> getOrderById(UUID orderId) {
    return orderRepository.findById(orderId);
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
        Order freshOrder = orderRepository.getById(matchingOrder.getId());

        if (freshOrder.isValidForMatching()) {
          return freshOrder;
        }

        orderBook.removeOrder(matchingOrder);
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

    orders.forEach(
        order -> {
          try {
            Optional<Order> freshOrder = orderRepository.findById(order.getId());
            if (freshOrder.isPresent() && freshOrder.get().isValidForMatching()) {
              validOrders.add(freshOrder.get());
            } else {
              orderBook.removeOrder(order);
            }
          } catch (Exception e) {
            log.warn("Failed to refresh buy order {}: {}", order.getId(), e.getMessage());
            orderBook.removeOrder(order);
          }
        });

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
        Optional<Order> freshOrder = orderRepository.findById(order.getId());
        if (freshOrder.isPresent() && freshOrder.get().isValidForMatching()) {
          validOrders.add(freshOrder.get());
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
    Order order = orderRepository.getById(orderId);
    if (!order.isActive() || !order.getUserId().equals(userId)) {
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
    return includeInactive
        ? orderRepository.findAllByUserIdOrderByTimestampDesc(userId, pageable)
        : orderRepository.findAllByUserIdAndActiveOrderByTimestampDesc(userId, true, pageable);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getOrderBookStatistics(Instant since) {
    var tickers = orderRepository.findAllTickerCounts(since);
    var tickerCandles = orderRepository.findAllPriceRanges(since);

    return Map.of("orderCounts", tickers, "priceRanges", tickerCandles);
  }

  private OrderBook getOrCreateOrderBook(String ticker) {
    return Optional.ofNullable(orderBooks.get(ticker)).orElse(createOrderBook(ticker));
  }

  @Locked.Write
  private OrderBook createOrderBook(String ticker) {
    return orderBooks.computeIfAbsent(ticker, OrderBook::new);
  }
}
