package io.github.diogohmcruz.stockexchange.domain.model;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticker {
  private String ticker;
  private OrderType type;
  private Long count;
}
