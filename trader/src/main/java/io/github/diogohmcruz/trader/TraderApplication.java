package io.github.diogohmcruz.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.github.diogohmcruz")
@EnableAsync
@EnableScheduling
public class TraderApplication {
  public static void main(String[] args) {
    SpringApplication.run(TraderApplication.class, args);
  }
}
