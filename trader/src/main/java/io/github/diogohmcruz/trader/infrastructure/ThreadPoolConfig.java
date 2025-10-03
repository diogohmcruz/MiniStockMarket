package io.github.diogohmcruz.trader.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.github.diogohmcruz.marketlibrary.infrastructure.SimpleThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {
    @Bean(name = "traderTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        var cores = Runtime.getRuntime().availableProcessors();
        log.debug("Available processors: {}", cores);
        var maxMemory = Runtime.getRuntime().maxMemory();
        log.debug("Max heap memory (MB): {}", (maxMemory / 1024 / 1024));
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores * 20);
        executor.setMaxPoolSize(cores * 40);
        executor.setQueueCapacity(1000);
        executor.setThreadGroupName("traders");
        executor.setThreadNamePrefix("trader-");
        executor.setThreadFactory(
                SimpleThreadFactory.builder().nameFormat("trader-%d").build());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
