package io.github.diogohmcruz.marketlibrary.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Order details response")
public class OrderResponse {
  @Schema(description = "Unique identifier of the order")
  private UUID id;

  @Schema(description = "Type of order (BUY/SELL)")
  private OrderType type;

  @Schema(description = "Stock ticker symbol")
  private String ticker;

  @Schema(description = "Order price per unit")
  private BigDecimal price;

  @Schema(description = "Number of shares to trade")
  private int quantity;

  @Schema(description = "Time when the order was created")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
  private Instant timestamp;

  @Schema(description = "Time when the order will expire")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
  private Instant expirationTime;

  @Schema(description = "User ID of the trader")
  private String userId;

  @Schema(description = "Whether the order is still active")
  private boolean active;
}
