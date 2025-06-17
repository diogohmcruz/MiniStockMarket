package io.github.diogohmcruz.marketlibrary.infrastructure;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Builder;

@Builder
public class SimpleThreadFactory implements ThreadFactory {
  private static final AtomicInteger threadNumber = new AtomicInteger(1);
  private String nameFormat;

  @Override
  public Thread newThread(Runnable runnable) {
    var name = String.format(this.nameFormat, threadNumber.getAndIncrement());
    return new Thread(runnable, name);
  }
}
