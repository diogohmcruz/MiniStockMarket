package io.github.diogohmcruz.marketlibrary.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Information about a stock ticker")
public class TickerInfo {
  @Schema(description = "Stock ticker symbol", example = "AAPL")
  private String symbol;

  @Schema(description = "Latest trade price", example = "150.50")
  private BigDecimal lastPrice;

  @Schema(description = "Number of active buy orders", example = "5")
  private int activeBuyOrders;

  @Schema(description = "Number of active sell orders", example = "3")
  private int activeSellOrders;

  @Schema(description = "Time of the last trade")
  private Instant lastTradeTime;
}
