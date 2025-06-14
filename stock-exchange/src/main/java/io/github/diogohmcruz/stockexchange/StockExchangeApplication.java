package io.github.diogohmcruz.stockexchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.github.diogohmcruz")
@EnableAsync
@EnableScheduling
public class StockExchangeApplication {
  public static void main(String[] args) {
    SpringApplication.run(StockExchangeApplication.class, args);
  }
}
