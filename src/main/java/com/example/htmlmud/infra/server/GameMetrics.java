package com.example.htmlmud.infra.server;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class GameMetrics {
  // 統計當前 Tick 處理了多少指令
  private static AtomicInteger commandCount = new AtomicInteger(0);

  // 統計最近一次 Tick 花費的時間 (奈秒)
  private static long lastTickDuration = 0;

  // 統計平均 Tick 時間 (用於平滑顯示)
  private static long averageTickDuration = 0;

  // 當有玩家輸入指令被執行時，呼叫此方法
  public static void incrementCommand() {
    commandCount.incrementAndGet();
  }

  public static int getAndResetCommandCount() {
    return commandCount.getAndSet(0);
  }

  public static void updateTickDuration(long durationNanos) {
    lastTickDuration = durationNanos;
    // 簡單的移動平均計算 (90% 舊值 + 10% 新值)，避免數字跳動太劇烈
    if (averageTickDuration == 0)
      averageTickDuration = durationNanos;
    else
      averageTickDuration = (long) (averageTickDuration * 0.9 + durationNanos * 0.1);
  }

  // Getters...
  public static long getLastTickMs() {
    return lastTickDuration / 1_000_000;
  }
}
