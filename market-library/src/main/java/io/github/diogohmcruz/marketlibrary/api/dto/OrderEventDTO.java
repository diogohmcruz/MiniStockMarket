package io.github.diogohmcruz.marketlibrary.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Lifecycle event of an order, published through the outbox")
public class OrderEventDTO {
    @Schema(description = "What happened to the order")
    private OrderEventType type;

    @Schema(description = "Snapshot of the order after the event")
    private OrderDTO order;

    @Schema(description = "Trade that caused the event (FILL only)")
    private UUID tradeId;

    @Schema(description = "Shares executed by the trade (FILL only)")
    private Integer quantity;

    @Schema(description = "Execution price (FILL only)")
    private BigDecimal price;
}
