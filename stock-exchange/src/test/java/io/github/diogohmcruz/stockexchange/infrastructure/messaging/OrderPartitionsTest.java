package io.github.diogohmcruz.stockexchange.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.kafka.clients.producer.internals.BuiltInPartitioner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderPartitionsTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 6, 12, 50})
    void mapsTickersToTheSamePartitionAsKafkaDoes(Integer partitionsCount) {
        var tickersNames = List.of("AAPL", "GOOGL", "MSFT", "TSLA", "A", "ZZZZZ", "NVDA", "AMZN");
        tickersNames.forEach(ticker -> {
            var tickerBytes = ticker.getBytes(StandardCharsets.UTF_8);
            var actual = OrderPartitions.partitionFor(ticker, partitionsCount);
            var expected = BuiltInPartitioner.partitionForKey(tickerBytes, partitionsCount);
            assertThat(actual)
                    .as("%s over %d partitions", ticker, partitionsCount)
                    .isEqualTo(expected);
        });
    }
}
