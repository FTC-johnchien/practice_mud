package com.example.htmlmud.infra.monitor;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.LongAdder;

@Component
public class GameMetrics {
  private final LongAdder playerCommands = new LongAdder();
  private final LongAdder systemTasks = new LongAdder();
  private final LongAdder totalPulseDurationNanos = new LongAdder();

  // 增加玩家輸入指令計數
  public void incrementPlayerCommand() {
    playerCommands.increment();
  }

  // 增加系統任務計數 (如戰鬥動作、AI、Queue 處理)
  public void incrementSystemTask() {
    systemTasks.increment();
  }

  // 累加心跳耗時 (奈秒)
  public void addPulseDurationNanos(long nanos) {
    totalPulseDurationNanos.add(nanos);
  }

  public long getPlayerCommands() {
    return playerCommands.sum();
  }

  public long getSystemTasks() {
    return systemTasks.sum();
  }

  public long getTotalPulseDurationNanos() {
    return totalPulseDurationNanos.sum();
  }

  public void resetMetrics() {
    playerCommands.reset();
    systemTasks.reset();
    totalPulseDurationNanos.reset();
  }
}
