package io.github.diogohmcruz.api.dto;

import io.github.diogohmcruz.domain.model.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to create a new order")
public class CreateOrderRequest {

  @Schema(description = "Type of order (BUY/SELL)", example = "BUY")
  @NotNull(message = "Order type is required")
  private OrderType type;

  @Schema(description = "Stock ticker symbol", example = "AAPL")
  @NotBlank(message = "Ticker is required")
  @Pattern(regexp = "^[A-Z]{1,5}$", message = "Ticker must be 1-5 uppercase letters")
  private String ticker;

  @Schema(description = "Order price per unit", example = "150.50")
  @NotNull(message = "Price is required")
  @Min(value = 0, message = "Price must be greater than 0")
  private BigDecimal price;

  @Schema(description = "Number of shares to trade", example = "100")
  @NotNull(message = "Quantity is required")
  @Min(value = 1, message = "Quantity must be at least 1")
  private int quantity;

  @Schema(description = "Time-to-live in seconds", example = "3600")
  @Min(value = 60, message = "TTL must be at least 60 seconds")
  private int ttlSeconds = 3600;
}
