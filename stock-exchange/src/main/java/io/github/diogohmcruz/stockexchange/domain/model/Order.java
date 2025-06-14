package io.github.diogohmcruz.stockexchange.domain.model;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Order type is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderType type;

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
  private boolean active = true;

  @Version
  @Column(nullable = false)
  private Long version = 0L;

  @PrePersist
  protected void onCreate() {
    if (timestamp == null) {
      timestamp = Instant.now();
    }
    if (expirationTime == null) {
      expirationTime = timestamp.plusSeconds(3600);
    }
    if (version == null) {
      version = 0L;
    }
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expirationTime);
  }

  public boolean isValidForMatching() {
    return active && !isExpired();
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
