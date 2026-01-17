package com.example.htmlmud.service.world;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.MudKeys;
import com.example.htmlmud.domain.model.map.Area;
import com.example.htmlmud.domain.model.map.Room;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WorldManager {

  private final ResourcePatternResolver resourceResolver;

  // 1. Static Data Cache: 存放唯讀的 Room 設定檔 (POJO/Record)
  // 雖然伺服器通常會載入全地圖，但 Caffeine 可以幫我們管理記憶體上限
  private final Cache<Integer, Room> staticRoomCache;

  // 2. Runtime Actors: 存放正在運作的 RoomActor
  // 使用 ConcurrentHashMap 確保並發存取安全
  private final ConcurrentHashMap<Integer, RoomActor> activeRooms = new ConcurrentHashMap<>();

  // 3. Write-Behind Queue: 存放待寫入資料庫的變更
  private final BlockingQueue<RoomStateUpdate> persistenceQueue = new LinkedBlockingQueue<>();

  // 快取：ID -> Player 實體
  private final ConcurrentHashMap<String, PlayerActor> activePlayers = new ConcurrentHashMap<>();

  private volatile boolean isRunning = true;

  @Autowired
  private ObjectMapper objectMapper;

  public WorldManager(ResourcePatternResolver resourceResolver) {
    this.resourceResolver = resourceResolver;

    // 設定 Caffeine Cache
    this.staticRoomCache = Caffeine.newBuilder().maximumSize(10_000) // 假設地圖上限
        .expireAfterAccess(1, TimeUnit.HOURS) // 沒人用的房間資料可被釋放 (視需求)
        .recordStats().build();
  }

  /**
   * 伺服器啟動時載入地圖
   */
  @PostConstruct
  public void loadWorld() {
    log.info("Starting World Loading...");
    try {
      // 讀取 classpath:maps/ 下所有的 json
      Resource[] resources = resourceResolver.getResources("classpath:maps/*.json");

      for (Resource res : resources) {
        try {
          Area area = objectMapper.readValue(res.getInputStream(), Area.class);
          log.info("Loading Area: {} ({}) - {} rooms", area.name(), area.id(), area.rooms().size());

          // 將房間放入 Cache，建立全域索引
          for (Room room : area.rooms()) {
            staticRoomCache.put(room.id(), room);
          }
        } catch (IOException e) {
          log.error("Failed to load map file: {}", res.getFilename(), e);
        }
      }
    } catch (IOException e) {
      log.error("Could not read map resources", e);
    }

    // 啟動 Write-Behind 消費者執行緒
    startPersistenceWorker();
  }

  /**
   * 核心方法：取得或創建 RoomActor 這是進入遊戲世界的入口
   */
  public RoomActor getRoomActor(int roomId) {
    // 1. 如果 Actor 已經存在，直接回傳
    return activeRooms.computeIfAbsent(roomId, id -> {
      // 2. 如果 Actor 不存在，從 Static Cache 拿資料並 new 一個
      Room roomData = staticRoomCache.getIfPresent(id);
      if (roomData == null) {
        throw new IllegalArgumentException("Room ID not found: " + id);
      }
      // 創建新的 Actor，並注入 persistence callback
      // return new RoomActor(roomData, this::enqueueSave);
      return new RoomActor(roomData);
    });
  }

  // ==========================================
  // Write-Behind Implementation (非同步存檔機制)
  // ==========================================

  // 定義存檔請求的封包 (Record)
  public record RoomStateUpdate(int roomId, String dataToSave) {
  }

  public void addPlayer(PlayerActor actor) {
    activePlayers.put(actor.getId(), actor);
  }

  public PlayerActor getPlayer(WebSocketSession session) {
    String playerId = (String) session.getAttributes().get(MudKeys.PLAYER_ID);
    return getPlayer(playerId);
  }

  public PlayerActor getPlayer(String playerId) {
    // TODO 若 playerActor 不存在，則需要從資料庫載入並建立

    return activePlayers.get(playerId);
  }

  /**
   * RoomActor 呼叫此方法來請求存檔，不會阻塞 Actor 的運作
   */
  public void enqueueSave(int roomId, String data) {
    persistenceQueue.offer(new RoomStateUpdate(roomId, data));
  }

  private void startPersistenceWorker() {
    // 使用 Java 21+ Virtual Thread 來處理後台存檔
    Thread.ofVirtual().name("world-persistence-worker").start(() -> {
      log.info("World Persistence Worker started (Virtual Thread)");
      while (isRunning) {
        try {
          // 阻塞直到有資料進來
          RoomStateUpdate update = persistenceQueue.take();
          processUpdate(update);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });
  }

  private void processUpdate(RoomStateUpdate update) {
    // 模擬寫入資料庫 (MySQL JSON)
    // 未來這裡會注入 JPA Repository
    try {
      // 模擬 IO 延遲
      Thread.sleep(10);
      log.debug("[Write-Behind] Saved room {} state to DB: {}", update.roomId(),
          update.dataToSave());
    } catch (Exception e) {
      log.error("Error saving room state", e);
    }
  }

  @PreDestroy
  public void shutdown() {
    isRunning = false;
    // 實際專案中這裡應該要把 Queue 裡的剩餘資料 flush 到資料庫
    log.info("WorldManager shutting down...");
  }
}
