package io.github.diogohmcruz.livemarket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.github.diogohmcruz.livemarket.service.TickerStatsCache;
import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;

@SpringBootTest
@Testcontainers
class LiveMarketKafkaIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("redpandadata/redpanda:latest"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "live-market-it");
        registry.add("spring.autoconfigure.exclude", () -> ""); // re-enable kafka autoconfig
        registry.add(
                "spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.UUIDDeserializer");
        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add(
                "spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "io.github.diogohmcruz.marketlibrary.api.dto.TradeEvent");
        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    }

    @Autowired
    KafkaTemplate<UUID, TradeDTO> kafkaTemplate;

    @Autowired
    TickerStatsCache cache;

    @Test
    @Timeout(30)
    void consumingTradeEventUpdatesCache() {
        var trade = new TradeDTO();
        trade.setId(UUID.randomUUID());
        trade.setTicker("AAPL");
        trade.setPrice(new BigDecimal("123.45"));
        trade.setQuantity(25);
        trade.setBuyerId("buyer-1");
        trade.setSellerId("seller-1");
        trade.setTimestamp(Instant.now());

        kafkaTemplate.send("trades", trade.getId(), trade);

        Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var snapshot = cache.getTicker("AAPL");
            assertThat(snapshot).isNotNull();
            assertThat(snapshot.getLastPrice()).isEqualByComparingTo("123.45");
        });
    }
}
