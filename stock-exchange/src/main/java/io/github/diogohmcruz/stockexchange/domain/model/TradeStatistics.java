package io.github.diogohmcruz.stockexchange.domain.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class TradeStatistics {
  private int tradeCount;
  private BigDecimal minPrice;
  private BigDecimal maxPrice;
  private BigDecimal avgPrice;
  private int totalVolume;
}
