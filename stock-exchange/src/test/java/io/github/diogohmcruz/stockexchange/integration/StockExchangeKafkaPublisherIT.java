package io.github.diogohmcruz.stockexchange.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.github.diogohmcruz.marketlibrary.domain.model.OrderType;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.model.Trade;
import io.github.diogohmcruz.stockexchange.domain.service.TradeService;

@SpringBootTest
@Testcontainers
@TestPropertySource(
        properties = {"live-market.kafka.enabled=true", "simulation.enabled=false"
            // Use default trade topic
        })
class StockExchangeKafkaPublisherIT {

    private static final String TOPIC = "trades";

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("redpandadata/redpanda:latest"));

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.autoconfigure.exclude", () -> "");
        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    }

    @Autowired
    TradeService tradeService;

    private static Consumer<UUID, Trade> consumer;

    @BeforeAll
    static void setupConsumer() {
        var props = KafkaTestUtils.consumerProps("stock-exchange-it-consumer", "true", kafka.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Trade.class.getName());
        consumer = new DefaultKafkaConsumerFactory<UUID, Trade>(props).createConsumer();
        consumer.subscribe(java.util.List.of(TOPIC));
    }

    @AfterAll
    static void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishingTradeEmitsKafkaEvent() {
        var buy = new Order();
        buy.setType(OrderType.BUY);
        buy.setTicker("AMZN");
        buy.setPrice(new BigDecimal("101.25"));
        buy.setQuantity(10);
        buy.setExpirationTime(Instant.now().plusSeconds(3600));
        buy.setUserId("buyer-" + UUID.randomUUID());

        var sell = new Order();
        sell.setType(OrderType.SELL);
        sell.setTicker("AMZN");
        sell.setPrice(new BigDecimal("101.25"));
        sell.setQuantity(10);
        sell.setExpirationTime(Instant.now().plusSeconds(3600));
        sell.setUserId("seller-" + UUID.randomUUID());

        var trade = new Trade(buy, sell, buy.getPrice());
        tradeService.processTrade(trade);

        var received = new AtomicBoolean(false);
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var records = consumer.poll(Duration.ofMillis(250));
            records.forEach(rec -> {
                if (rec.value() != null && "AMZN".equals(rec.value().getTicker())) {
                    received.set(true);
                }
            });
            assertThat(received.get()).isTrue();
        });
    }
}
