package io.github.diogohmcruz.stockexchange.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ColumnDefault("'MAIN'")
    @Column(nullable = false, length = 10)
    private String venue;

    @NotBlank(message = "Ticker symbol is required")
    @Pattern(regexp = "^[A-Z]{1,5}$", message = "Ticker must be 1-5 uppercase letters")
    @Column(nullable = false)
    private String ticker;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(nullable = false)
    private BigDecimal price;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Instant timestamp;

    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "sell_order_id", nullable = false)
    private Order sellOrder;

    @NotBlank(message = "Buyer ID is required")
    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @NotBlank(message = "Seller ID is required")
    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Version
    @Column(nullable = false)
    private Long version;

    public Trade(Order buyOrder, Order sellOrder, BigDecimal executionPrice) {
        this.venue = buyOrder.getVenue();
        this.ticker = buyOrder.getTicker();
        this.price = executionPrice;
        this.quantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());
        this.timestamp = Instant.now();
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.buyerId = buyOrder.getUserId();
        this.sellerId = sellOrder.getUserId();
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
