package com.example.htmlmud.infra.monitor;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.LongAdder;

@Component
public class GameMetrics {
  private final LongAdder processedCommands = new LongAdder();
  private final LongAdder lastPulseDurationMs = new LongAdder();

  // 增加指令計數
  public void incrementCommand() {
    processedCommands.increment();
  }

  // 記錄心跳耗時
  public void recordPulseDuration(long ms) {
    lastPulseDurationMs.reset();
    lastPulseDurationMs.add(ms);
  }

  public long getProcessedCommands() {
    return processedCommands.sum();
  }

  public long getLastPulseDurationMs() {
    return lastPulseDurationMs.sum();
  }

  public void resetCommands() {
    processedCommands.reset();
  }
}
