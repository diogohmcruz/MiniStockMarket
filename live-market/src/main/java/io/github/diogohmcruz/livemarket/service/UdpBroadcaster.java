package io.github.diogohmcruz.livemarket.service;

import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** A simple UDP broadcaster using Netty. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UdpBroadcaster {

    private final ObjectMapper objectMapper;

    private Bootstrap bootstrap;
    private Channel channel;
    private EventLoopGroup group;
    private InetSocketAddress target;

    @Value("${live-market.udp.host}")
    private String multicastHost;

    @Value("${live-market.udp.port}")
    private int port;

    private synchronized void initIfNecessary() {
        if (bootstrap != null) {
            return;
        }
        group = new NioEventLoopGroup(1);
        target = new InetSocketAddress(multicastHost, port);
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        // Se quiser tratar respostas, adicionar handlers aqui.
                        // Exemplo de no-op inbound:
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                                // Ignorado (somente broadcast outbound)
                            }
                        });
                    }
                });
        try {
            channel = bootstrap.bind(0).sync().channel();
            log.info("Initialized UDP broadcaster target={} port={}", multicastHost, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to initialize UdpBroadcaster", e);
        }
    }

    public void send(Object payload) {
        initIfNecessary();
        try {
            var msg = (payload instanceof String) ? (String) payload : objectMapper.writeValueAsString(payload);
            var packet = new DatagramPacket(Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8), target);
            channel.writeAndFlush(packet).addListener(f -> {
                if (!f.isSuccess()) {
                    log.warn("Failed to send UDP packet: {}", f.cause().getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payload for UDP broadcast", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
