package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

import lombok.experimental.UtilityClass;
import org.apache.kafka.common.utils.Utils;

/**
 * Mirrors Kafka's default partitioner for keyed records.
 * <p>
 * The engine can tell which instruments (order books) belong to the partitions it was assigned.
 * <p>
 * Orders are keyed by instrument ({@code venue:ticker}), hence one book maps to exactly one partition.
 */
@UtilityClass
public final class OrderPartitions {

    public static int partitionFor(String instrument, int numPartitions) {
        var instrumentBytes = instrument.getBytes(StandardCharsets.UTF_8);
        var murmured = Utils.murmur2(instrumentBytes);
        return Utils.toPositive(murmured) % numPartitions;
    }
}
