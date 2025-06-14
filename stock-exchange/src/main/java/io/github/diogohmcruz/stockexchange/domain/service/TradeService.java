package io.github.diogohmcruz.stockexchange.domain.service;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import io.github.diogohmcruz.stockexchange.domain.model.TradeStatistics;
import io.github.diogohmcruz.stockexchange.domain.repositories.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class TradeService {

  private final TradeRepository tradeRepository;

  @Transactional
  public Trade saveTrade(@Valid Trade trade) {
    Trade savedTrade = tradeRepository.save(trade);

    log.info(
        "Trade executed: [{}] {}x {} at {} between {} and {}",
        savedTrade.getId(),
        savedTrade.getQuantity(),
        savedTrade.getTicker(),
        savedTrade.getPrice(),
        savedTrade.getBuyerId(),
        savedTrade.getSellerId());

    return savedTrade;
  }

  @Transactional(readOnly = true)
  public Optional<Trade> getTradeById(UUID tradeId) {
    return tradeRepository.findById(tradeId);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "recentTrades", key = "#ticker", condition = "#ticker != null")
  public List<Trade> getTradesByTicker(String ticker) {
    return tradeRepository.findByTickerOrderByTimestampDesc(ticker);
  }

  @Transactional(readOnly = true)
  public Page<Trade> getTradesByUser(String userId, Pageable pageable) {
    return tradeRepository.findByBuyerIdOrSellerIdOrderByTimestampDesc(userId, userId, pageable);
  }

  @Transactional(readOnly = true)
  public List<Trade> getTradesByUserInTimeRange(String userId, Instant startTime, Instant endTime) {
    return tradeRepository.findTradesByUserInDateRange(userId, startTime, endTime);
  }

  @Transactional(readOnly = true)
  public List<Trade> getRecentTradesByTicker(String ticker, int limit) {
    return tradeRepository.findRecentTradesByTicker(ticker).stream().limit(limit).toList();
  }

  @Transactional(readOnly = true)
  public long countRecentTrades(String ticker, Instant since) {
    return tradeRepository.countRecentTrades(ticker, since);
  }

  @Transactional(readOnly = true)
  public Map<String, BigDecimal> getLatestPrices() {
    Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
    Map<String, BigDecimal> latestPrices = new HashMap<>();

    List<Trade> recentTrades =
        tradeRepository.findAllByTimestampGreaterThanOrderByTimestampDesc(oneDayAgo);

    recentTrades.stream()
        .filter(trade -> !latestPrices.containsKey(trade.getTicker()))
        .forEach(trade -> latestPrices.put(trade.getTicker(), trade.getPrice()));

    return latestPrices;
  }

  @Transactional(readOnly = true)
  public Map<String, TradeStatistics> getTickerStatistics(Instant since) {
    Map<String, List<Trade>> tradesByTicker =
        tradeRepository.findAllByTimestampGreaterThanEqual(since).stream()
            .collect(Collectors.groupingBy(Trade::getTicker));

    return tradesByTicker.entrySet().stream()
        .collect(
            HashMap::new,
            (map, entry) -> {
              List<Trade> tickerTrades = entry.getValue();
              BigDecimal minPrice =
                  tickerTrades.stream()
                      .map(Trade::getPrice)
                      .min(BigDecimal::compareTo)
                      .orElse(BigDecimal.ZERO);

              BigDecimal maxPrice =
                  tickerTrades.stream()
                      .map(Trade::getPrice)
                      .max(BigDecimal::compareTo)
                      .orElse(BigDecimal.ZERO);

              BigDecimal avgPrice =
                  tickerTrades.stream()
                      .map(Trade::getPrice)
                      .reduce(BigDecimal.ZERO, BigDecimal::add)
                      .divide(BigDecimal.valueOf(tickerTrades.size()), 2, RoundingMode.HALF_UP);

              int totalVolume = tickerTrades.stream().mapToInt(Trade::getQuantity).sum();

              map.put(
                  entry.getKey(),
                  new TradeStatistics(
                      tickerTrades.size(), minPrice, maxPrice, avgPrice, totalVolume));
            },
            HashMap::putAll);
  }

  @Transactional(readOnly = true)
  public Map<String, Integer> getDailyTradingVolume() {
    Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
    List<Trade> trades = tradeRepository.findAllByTimestampGreaterThanEqual(oneDayAgo);

    return trades.stream()
        .collect(
            HashMap::new,
            (map, trade) -> map.merge(trade.getTicker(), trade.getQuantity(), Integer::sum),
            HashMap::putAll);
  }

  @Transactional(readOnly = true)
  public List<Trade> getUserRecentTrades(String userId, int limit) {
    return tradeRepository.findByBuyerIdOrSellerIdOrderByTimestampDesc(userId, userId).stream()
        .limit(limit)
        .toList();
  }
}
