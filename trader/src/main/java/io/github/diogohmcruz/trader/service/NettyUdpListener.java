package io.github.diogohmcruz.trader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.udp.MulticastReceivingChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class NettyUdpListener {

    @Value("${live-market.udp.host}")
    private String multicastHost;

    @Value("${live-market.udp.port}")
    private int port;

    @Bean
    public MessageChannel multicastInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel deserializedChannel() {
        return new DirectChannel();
    }

    @Bean
    public MulticastReceivingChannelAdapter multicastAdapter() {
        var adapter = new MulticastReceivingChannelAdapter(multicastHost, port);
        adapter.setOutputChannel(multicastInputChannel());
        return adapter;
    }

    /** Transformer: byte[] → Order */
    @Bean
    @Transformer(inputChannel = "multicastInputChannel", outputChannel = "deserializedChannel")
    public org.springframework.integration.transformer.Transformer jsonToOrderTransformer(ObjectMapper objectMapper) {
        return message -> {
            try {
                byte[] payload = (byte[]) message.getPayload();
                var tickerInfo = objectMapper.readValue(payload, String.class);
                return MessageBuilder.withPayload(tickerInfo)
                        .copyHeaders(message.getHeaders())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize", e);
            }
        };
    }

    /** Final handler: receives Order objects */
    @Bean
    @ServiceActivator(inputChannel = "deserializedChannel")
    public MessageHandler orderHandler() {
        return message -> {
            var tickerInfo = (String) message.getPayload();
            log.info("📩 Received Order: {}", tickerInfo);
        };
    }
}
