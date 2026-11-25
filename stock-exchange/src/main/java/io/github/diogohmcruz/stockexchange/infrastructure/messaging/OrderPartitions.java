package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.utils.Utils;

/**
 * Mirrors Kafka's default partitioner for keyed records, so the engine can tell which tickers (order books) belong to
 * the partitions it was assigned. Orders are keyed by ticker, hence one book maps to exactly one partition.
 */
public final class OrderPartitions {

    private OrderPartitions() {}

    public static int partitionFor(String ticker, int numPartitions) {
        return Utils.toPositive(Utils.murmur2(ticker.getBytes(StandardCharsets.UTF_8))) % numPartitions;
    }
}
