package com.example.htmlmud.service.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.actor.RoomActor;
import com.example.htmlmud.domain.context.MudKeys;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.domain.actor.MobActor;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.domain.model.map.Area;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.infra.mapper.ItemTemplateMapper;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.map.MobReset;
import com.example.htmlmud.infra.mapper.PlayerMapper;
import com.example.htmlmud.infra.persistence.entity.ItemTemplateEntity;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;
import com.example.htmlmud.infra.persistence.entity.RoomEntity;
import com.example.htmlmud.infra.persistence.repository.ItemTemplateRepository;
import com.example.htmlmud.infra.persistence.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldManager {

  // private final GameServices gameServices;
  private final ObjectMapper objectMapper;
  private final ResourcePatternResolver resourceResolver;
  private final RoomRepository roomRepository;
  private final ItemTemplateRepository itemTemplateRepository;
  private final ItemTemplateMapper itemTemplateMapper;

  // 1. Static Data Cache: 存放唯讀的 Room 設定檔 (POJO/Record)
  // 雖然伺服器通常會載入全地圖，但 Caffeine 可以幫我們管理記憶體上限
  private final Cache<Integer, RoomTemplate> staticRoomCache =
      Caffeine.newBuilder().maximumSize(10_000) // 假設地圖上限
          .expireAfterAccess(1, TimeUnit.HOURS) // 沒人用的房間資料可被釋放 (視需求)
          .recordStats().build();

  // 怪物原型快取
  private final Cache<Integer, MobTemplate> staticMobCache =
      Caffeine.newBuilder().maximumSize(5_000).expireAfterAccess(1, TimeUnit.HOURS).build();

  // 2. Runtime Actors: 存放正在運作的 RoomActor
  // 使用 ConcurrentHashMap 確保並發存取安全
  private final ConcurrentHashMap<Integer, RoomActor> activeRooms = new ConcurrentHashMap<>();

  // 3. Write-Behind Queue: 存放待寫入資料庫的變更
  private final BlockingQueue<RoomStateUpdate> persistenceQueue = new LinkedBlockingQueue<>();

  // 快取：ID -> Player 實體
  private final ConcurrentHashMap<String, PlayerActor> activePlayers = new ConcurrentHashMap<>();

  private volatile boolean isRunning = true;



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
          for (RoomTemplate room : area.rooms()) {
            staticRoomCache.put(room.id(), room);
          }

          // 將怪物原型放入 Cache
          log.info("area.mobTemplates().size: {}", area.mobTemplates().size());
          for (MobTemplate mob : area.mobTemplates()) {
            staticMobCache.put(mob.id(), mob);
          }

          // 3. 根據 mob_resets 產生怪物實體
          if (area.mobResets() != null) {
            for (MobReset reset : area.mobResets()) {

              MobTemplate mobTpl = staticMobCache.getIfPresent(reset.mobTemplateId());

              if (mobTpl != null) {
                RoomActor roomActor = getRoomActor(reset.roomId());

                for (int i = 0; i < reset.maxQty(); i++) {
                  // 建立 MobActor 並設定初始房間
                  MobActor mobActor = new MobActor(mobTpl, new LivingState(), null);
                  log.info("Spawned Mob: {} in Room: {}", mobTpl.name(), reset.roomId());
                  mobActor.setCurrentRoomId(reset.roomId());
                  log.info("MobTemplate:{}", objectMapper.writeValueAsString(mobActor));

                  // 把 mob 塞到房間里
                  roomActor.getMobs().add(mobActor);
                  mobActor.start();
                }
              } else {
                log.info("MobTemplate is null");
              }
            }
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

  // 啟動時載入世界
  @PostConstruct
  public void loadWorld2() {
    List<RoomEntity> entities = roomRepository.findAll();

    for (RoomEntity e : entities) {
      // 1. 構建靜態 Template
      RoomTemplate tpl = new RoomTemplate(e.getId(), e.getName(), e.getDescription(),
          new ArrayList<>(), e.getExits(), new ConcurrentHashMap<>());

      // 2. 構建動態 Actor (注入掉落物)
      RoomActor actor = new RoomActor(tpl, e.getDroppedItems());

      activeRooms.put(e.getId(), actor);
    }
  }

  /**
   * 核心方法：取得或創建 RoomActor 這是進入遊戲世界的入口
   */
  public RoomActor getRoomActor(int roomId) {
    log.info("getRoomActor roomId: {}", roomId);
    // 1. 如果 Actor 已經存在，直接回傳
    return activeRooms.computeIfAbsent(roomId, id -> {
      // 2. 如果 Actor 不存在，從 Static Cache 拿資料並 new 一個
      RoomTemplate roomData = staticRoomCache.getIfPresent(id);
      if (roomData == null) {
        throw new IllegalArgumentException("Room ID not found: " + id);
      }
      // 創建新的 Actor，並注入 persistence callback
      // return new RoomActor(roomData, this::enqueueSave);
      return new RoomActor(roomData, new ArrayList<>());
    });
  }

  public MobTemplate getMobTemplate(int mobId) {
    return staticMobCache.getIfPresent(mobId);
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

  public void removePlayer(String playerId) {
    activePlayers.remove(playerId);
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

  /**
   * 對房間內的所有人廣播訊息 (除了來源者自己)
   *
   * @param roomId 房間 ID
   * @param message 訊息內容
   * @param excludeActorId 不想收到廣播的人 (通常是移動者本人)，可為 null
   */
  public void broadcastToRoom(Integer roomId, String message, String excludeActorId) {
    List<PlayerActor> actors = getActorsInRoom(roomId); // 假設您已有此方法

    for (PlayerActor actor : actors) {
      if (excludeActorId != null && actor.getId().equals(excludeActorId)) {
        continue;
      }
      // 直接發送文字
      actor.sendText(message);
    }
  }

  // 簡單的 getActorsInRoom 實作參考 (效率較差，之後可用 Map<RoomId, Set<ActorId>> 優化)
  public List<PlayerActor> getActorsInRoom(Integer roomId) {
    return activePlayers.values().stream().filter(a -> roomId.equals(a.getCurrentRoomId()))
        .toList();
  }

  /**
   * 建立一個全新的物品實體
   *
   * @param templateId 物品原型 ID
   * @param randomize 是否進行隨機數值浮動
   */
  public GameItem createItem(int templateId, boolean randomize) {
    ItemTemplate tpl = loadItemTemplate(templateId);

    GameItem item = new GameItem();
    item.setId(UUID.randomUUID().toString());
    item.setTemplateId(tpl.id());
    item.setCurrentDurability(tpl.maxDurability());
    item.setAmount(1);

    // --- 處理隨機邏輯 (RNG) ---
    if (randomize) {
      // 1. 隨機浮動耐久度 (例如：全新 ~ 80% 新)
      // item.setCurrentDurability(...);

      // 2. 隨機詞綴 (例如 10% 機率出現 +1 攻擊)
      if (ThreadLocalRandom.current().nextDouble() < 0.1) {
        item.getDynamicProps().put("attack_bonus", 1);
        item.getDynamicProps().put("quality", "RARE");
      }
    }

    return item;
  }

  public ItemTemplate loadItemTemplate(int templateId) {
    // 1. DB -> Entity
    ItemTemplateEntity entity = itemTemplateRepository.findById(templateId).orElseThrow(
        () -> new IllegalArgumentException("ItemTemplate ID not found: " + templateId));

    // 2. Entity -> Record (MapStruct 自動轉)
    // 注意：這裡得到的 Record 內含的 State 是 Entity 裡解序列化出來的
    return itemTemplateMapper.toRecord(entity);
  }
}
