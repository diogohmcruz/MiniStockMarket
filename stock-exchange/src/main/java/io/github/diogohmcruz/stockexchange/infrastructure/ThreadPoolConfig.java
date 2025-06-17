package io.github.diogohmcruz.stockexchange.infrastructure;

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
  @Bean(name = "brokerTaskExecutor")
  public ThreadPoolTaskExecutor taskExecutor() {
    int cores = Runtime.getRuntime().availableProcessors();
    log.debug("Available processors: {}", cores);
    long maxMemory = Runtime.getRuntime().maxMemory();
    log.debug("Max heap memory (MB): {}", (maxMemory / 1024 / 1024));
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(cores * 2);
    executor.setMaxPoolSize(cores * 4);
    executor.setQueueCapacity(1000);
    executor.setThreadGroupName("brokers");
    executor.setThreadNamePrefix("broker-");
    executor.setThreadFactory(SimpleThreadFactory.builder().nameFormat("broker-%d").build());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    executor.initialize();
    return executor;
  }
}
