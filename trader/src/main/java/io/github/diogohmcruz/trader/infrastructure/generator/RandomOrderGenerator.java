package io.github.diogohmcruz.trader.infrastructure.generator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.CreateOrderRequest;
import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;

@Component
public class RandomOrderGenerator {
  private final Random random = new Random();
  private final List<String> SAMPLE_TICKERS = List.of("AAPL", "GOOGL", "MSFT", "AMZN");

  public CreateOrderRequest generateOrderRequest() {
    OrderType type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
    String ticker = SAMPLE_TICKERS.get(random.nextInt(SAMPLE_TICKERS.size()));
    BigDecimal basePrice = new BigDecimal("100.00");
    BigDecimal variation = BigDecimal.valueOf(random.nextDouble() * 10);
    BigDecimal price = basePrice.add(variation);
    int quantity = 1 + random.nextInt(100);

    var request = new CreateOrderRequest();
    request.setType(type);
    request.setTicker(ticker);
    request.setPrice(price);
    request.setQuantity(quantity);
    return request;
  }
}
