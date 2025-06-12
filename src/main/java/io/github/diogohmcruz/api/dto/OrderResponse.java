package io.github.diogohmcruz.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.diogohmcruz.domain.model.Order;
import io.github.diogohmcruz.domain.model.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

  public static OrderResponse fromOrder(Order order) {
    OrderResponse response = new OrderResponse();
    response.setId(order.getId());
    response.setType(order.getType());
    response.setTicker(order.getTicker());
    response.setPrice(order.getPrice());
    response.setQuantity(order.getQuantity());
    response.setTimestamp(order.getTimestamp());
    response.setExpirationTime(order.getExpirationTime());
    response.setUserId(order.getUserId());
    response.setActive(order.isActive());
    return response;
  }
}