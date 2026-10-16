package com.example.htmlmud.infra.server;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.world.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    // startServerEngine();

    // 3. (未來) 載入 NPC 或 排行榜快取

    long duration = System.currentTimeMillis() - start;
    log.info("=== MUD World Ready in {} ms ===", duration);
  }

  // private void startServerEngine() {
  // // 1. 建立一個專門的執行緒給 Game Loop
  // // 注意：絕對不能直接在 run() 裡呼叫 serverEngine.run()
  // // 因為那樣會阻塞主執行緒，導致 Spring Boot 認為啟動還沒完成

  // engineThread = new Thread(serverEngine);

  // // 2. 幫執行緒取個名字，方便 Debug (看 Log 時會顯示這個名字)
  // engineThread.setName("MUD-GameLoop");

  // // 3. 啟動！
  // engineThread.start();

  // System.out.println("🚀 MUD 核心引擎執行緒已異步啟動。");
  // }

  // @PreDestroy
  // public void onExit() throws Exception {
  // System.out.println("正在關閉遊戲引擎...");
  // serverEngine.stop(); // 通知迴圈停止
  // try {
  // // 等待迴圈跑完最後一圈
  // engineThread.join(2000);
  // } catch (InterruptedException e) {
  // e.printStackTrace();
  // }
  // }

}
