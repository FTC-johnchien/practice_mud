package com.example.htmlmud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class SchedulerConfig {

  @Bean(destroyMethod = "shutdown") // 確保伺服器關閉時釋放資源
  public ScheduledExecutorService gameScheduler() {
    // 核心執行緒數設為 2~4 即可。
    // 因為我們只用它來 "觸發" 任務，實際執行會丟給 Virtual Thread，
    // 所以這個 Scheduler 幾乎不會被卡住，不需要太多執行緒。
    return Executors.newScheduledThreadPool(4, Thread.ofPlatform().factory());
  }
}
