package io.github.diogohmcruz.stockexchange.domain.model;

import java.math.BigDecimal;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public final class TickerCandle {
  private final String ticker;
  private final BigDecimal lowestAsk;
  private final BigDecimal highestBid;
}
