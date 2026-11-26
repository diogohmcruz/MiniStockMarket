package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import io.github.diogohmcruz.marketlibrary.api.dto.OrderDTO;
import io.github.diogohmcruz.stockexchange.domain.model.Order;
import io.github.diogohmcruz.stockexchange.domain.service.OrderBookService;
import io.github.diogohmcruz.stockexchange.domain.service.OrderMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Consumes {@link Order} messages and delegates to the {@link OrderMatchingService}. */
@Slf4j
@Component
@ConditionalOnProperty(name = "live-market.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OrderListener implements ConsumerSeekAware {

    private final OrderMatchingService matchingService;
    private final OrderBookService orderBookService;
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Value("${live-market.kafka.order-topic}")
    private String orderTopic;

    @KafkaListener(topics = "${live-market.kafka.order-topic}")
    public void onOrder(@Payload OrderDTO orderDTO, @Header(KafkaHeaders.OFFSET) long offset) {
        var order = Mappers.toNewOrder(orderDTO);
        order.setSequence(offset);
        matchingService.match(order);
    }

    /**
     * The books live in memory, so when this instance is handed partitions (startup or rebalance) the books of the
     * instruments hashing to them are rebuilt from the database before their first order is processed.
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        var partitions = orderPartitions(assignments.keySet());
        if (partitions.isEmpty()) {
            return;
        }
        var totalPartitions = kafkaTemplate.partitionsFor(orderTopic).size();
        var loaded = orderBookService.rebuildBooks(
                instrument -> partitions.contains(OrderPartitions.partitionFor(instrument, totalPartitions)));
        log.info("Assigned order partitions {}: rebuilt books with {} resting orders", partitions, loaded);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> revoked) {
        var partitions = orderPartitions(revoked);
        if (partitions.isEmpty()) {
            return;
        }
        var totalPartitions = kafkaTemplate.partitionsFor(orderTopic).size();
        orderBookService.dropBooks(
                instrument -> partitions.contains(OrderPartitions.partitionFor(instrument, totalPartitions)));
        log.info("Revoked order partitions {}: dropped their books", partitions);
    }

    private Set<Integer> orderPartitions(Collection<TopicPartition> topicPartitions) {
        return topicPartitions.stream()
                .filter(tp -> tp.topic().equals(orderTopic))
                .map(TopicPartition::partition)
                .collect(Collectors.toSet());
    }
}
