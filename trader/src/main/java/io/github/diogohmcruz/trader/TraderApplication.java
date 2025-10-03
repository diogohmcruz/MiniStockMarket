package io.github.diogohmcruz.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "io.github.diogohmcruz")
@EnableAsync
public class TraderApplication {
    public static void main(String[] args) {
        SpringApplication.run(TraderApplication.class, args);
    }
}
