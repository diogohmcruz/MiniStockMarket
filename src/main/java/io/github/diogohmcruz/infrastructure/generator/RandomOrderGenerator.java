package io.github.diogohmcruz.infrastructure.generator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.diogohmcruz.domain.model.Order;
import io.github.diogohmcruz.domain.model.OrderType;

@Component
public class RandomOrderGenerator {
  private final Random random = new Random();
  private final List<String> SAMPLE_TICKERS = List.of("AAPL", "GOOGL", "MSFT", "AMZN");

  public Order generateOrder() {
    OrderType type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
    String ticker = SAMPLE_TICKERS.get(random.nextInt(SAMPLE_TICKERS.size()));
    BigDecimal basePrice = new BigDecimal("100.00");
    BigDecimal variation = new BigDecimal(random.nextDouble() * 10);
    BigDecimal price = basePrice.add(variation);
    int quantity = 1 + random.nextInt(100);
    String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);

    return new Order(type, ticker, price, quantity, userId);
  }
}
