package io.github.diogohmcruz.trader.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
  @Bean(name = "traderTaskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadGroupName("traders");
    executor.setThreadNamePrefix("trader-");
    executor.setThreadFactory(TraderThreadFactory.builder().nameFormat("trader-%d").build());
    executor.initialize();
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    executor.initialize();
    return executor;
  }
}
