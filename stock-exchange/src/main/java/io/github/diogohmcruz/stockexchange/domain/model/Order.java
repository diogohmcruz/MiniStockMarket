package io.github.diogohmcruz.stockexchange.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;

import io.github.diogohmcruz.marketlibrary.domain.model.Instrument;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderStatus;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@Data
public class Order {
    /** Assigned when the order is created so it can travel through Kafka and be de-duplicated on redelivery. */
    @Id
    private UUID id = UUID.randomUUID();

    @NotNull(message = "Order type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @NotBlank(message = "Venue is required")
    @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Venue must be 2-10 uppercase letters or digits")
    @ColumnDefault("'MAIN'")
    @Column(nullable = false, length = 10)
    private String venue = Instrument.DEFAULT_VENUE;

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
    private Instant timestamp = Instant.now();

    @Future(message = "Expiration time must be in the future")
    @Column(nullable = false)
    private Instant expirationTime;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private int filledQuantity = 0;

    /** Position of the order in its ticker partition (Kafka offset); breaks price ties in the order book. */
    @Column
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.OPEN;

    /** Left null for new orders so Spring Data persists (instead of merging) them; Hibernate seeds it. */
    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (expirationTime == null) {
            expirationTime = timestamp.plusSeconds(3600);
        }
    }

    /** Key of the order book this order belongs to: {@code venue:ticker}. */
    public String getInstrument() {
        return Instrument.key(venue, ticker);
    }

    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public boolean isActive() {
        return status.isActive();
    }

    public void fill(int shares) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot fill order " + id + " in status " + status);
        }
        if (shares < 1 || shares > getRemainingQuantity()) {
            throw new IllegalArgumentException(
                    "Cannot fill " + shares + " shares, only " + getRemainingQuantity() + " remaining");
        }
        filledQuantity += shares;
        status = getRemainingQuantity() == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    /** Withdraws the unexecuted remainder. Returns false if the order was no longer active. */
    public boolean cancel() {
        return close(OrderStatus.CANCELLED);
    }

    /** Withdraws the unexecuted remainder after the expiration time. Returns false if no longer active. */
    public boolean expire() {
        return close(OrderStatus.EXPIRED);
    }

    private boolean close(OrderStatus terminal) {
        if (!isActive()) {
            return false;
        }
        status = terminal;
        return true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    public boolean isValidForMatching() {
        return isActive() && !isExpired();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (Order.class != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return getId() != null && Objects.equals(getId(), order.getId());
    }

    @Override
    public final int hashCode() {
        return Order.class.hashCode();
    }
}
