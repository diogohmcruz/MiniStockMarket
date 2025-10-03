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
        var type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
        var ticker = SAMPLE_TICKERS.get(random.nextInt(SAMPLE_TICKERS.size()));
        var basePrice = new BigDecimal("100.00");
        var variation = BigDecimal.valueOf(random.nextDouble() * 10);
        var price = basePrice.add(variation);
        var quantity = 1 + random.nextInt(100);

        var request = new CreateOrderRequest();
        request.setType(type);
        request.setTicker(ticker);
        request.setPrice(price);
        request.setQuantity(quantity);
        return request;
    }
}
