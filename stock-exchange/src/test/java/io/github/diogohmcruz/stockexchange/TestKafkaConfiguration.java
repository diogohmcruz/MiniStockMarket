package io.github.diogohmcruz.stockexchange;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.marketlibrary.api.dto.TradeDTO;

/**
 * Provides in-memory {@link KafkaTemplate} stand-ins when Kafka is disabled for tests (the Kafka auto-configuration is
 * excluded in {@code application.properties}), so the application context can still be created.
 */
@Configuration
@ConditionalOnProperty(name = "live-market.kafka.enabled", havingValue = "false")
class TestKafkaConfiguration {

    @Bean
    KafkaTemplate<String, OrderDTO> orderKafkaTemplate() {
        return completingTemplate();
    }

    @Bean
    KafkaTemplate<String, TradeDTO> tradeKafkaTemplate() {
        return completingTemplate();
    }

    @SuppressWarnings("unchecked")
    private static <V> KafkaTemplate<String, V> completingTemplate() {
        var template = (KafkaTemplate<String, V>) mock(KafkaTemplate.class);
        when(template.send(any(String.class), any(String.class), any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(new SendResult<>(
                        new ProducerRecord<>(
                                invocation.<String>getArgument(0),
                                invocation.<String>getArgument(1),
                                invocation.<V>getArgument(2)),
                        null)));
        return template;
    }
}
