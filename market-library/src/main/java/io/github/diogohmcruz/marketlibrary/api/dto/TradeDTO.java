package io.github.diogohmcruz.marketlibrary.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TradeDTO {
    private UUID id;
    private Long version = 0L;
    private Instant timestamp;
    private String ticker;
    private BigDecimal price;
    private int quantity;
    private UUID buyOrderId;
    private UUID sellOrderId;
    private String buyerId;
    private String sellerId;
}
