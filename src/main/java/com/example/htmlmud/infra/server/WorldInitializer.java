package com.example.htmlmud.infra.server;

import com.example.htmlmud.service.world.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1) // 如果有多個初始化步驟，可以控制順序
@RequiredArgsConstructor
public class WorldInitializer implements ApplicationRunner {

  private final WorldManager worldManager;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("=== MUD World Initialization Started ===");

    long start = System.currentTimeMillis();

    // 1. 載入地圖 (從原本 WorldManager 的 @PostConstruct 移過來)
    worldManager.loadWorld();

    // 2. (未來) 啟動全域計時器 (Tick Loop)
    // gameLoop.start();

    // 3. (未來) 載入 NPC 或 排行榜快取

    long duration = System.currentTimeMillis() - start;
    log.info("=== MUD World Ready in {} ms ===", duration);
  }
}
