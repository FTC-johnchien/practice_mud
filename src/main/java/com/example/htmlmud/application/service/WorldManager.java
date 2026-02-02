package com.example.htmlmud.application.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.map.RaceTemplate;
import com.example.htmlmud.domain.model.map.RoomExit;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.domain.model.map.SkillTemplate;
import com.example.htmlmud.domain.model.map.SpawnRule;
import com.example.htmlmud.domain.model.map.ZoneTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.infra.util.IdUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldManager {

  private final ResourcePatternResolver resourceResolver;

  private final ObjectMapper objectMapper;

  private final TemplateRepository templateRepo;

  private final WorldFactory worldFactory; // 注入 Factory

  // 2. Runtime Actors: 存放正在運作的 RoomActor
  // 使用 ConcurrentHashMap 確保並發存取安全
  @Getter
  private final ConcurrentHashMap<String, Room> activeRooms = new ConcurrentHashMap<>();

  // 快取：ID -> Living 實體
  private final ConcurrentHashMap<String, Living> activeLivings = new ConcurrentHashMap<>();

  // 3. Write-Behind Queue: 存放待寫入資料庫的變更
  private final BlockingQueue<RoomStateUpdate> persistenceQueue = new LinkedBlockingQueue<>();

  private volatile boolean isRunning = true;



  /**
   * 伺服器啟動時載入地圖
   */
  public void loadWorld() {

    // 讀取 global 資料
    loadgGlobalData();

    // 讀取 newbie_village 資料
    readZone("newbie_village");



    // 啟動 Write-Behind 消費者執行緒
    // startPersistenceWorker();

    // load zone
    // loadZone("newbie_village");
  }

  private void loadgGlobalData() {
    log.info("loadgGlobalData");

    loadgSkillData();

    loadRaceData();
  }



  private void loadRaceData() {
    log.info("load RaceData");

    try {
      Resource resource = resourceResolver.getResource("classpath:data/global/races.json");
      if (resource == null) {
        log.error("race not found");
        return;
      }

      Set<RaceTemplate> list = objectMapper.readValue(resource.getInputStream(),
          new TypeReference<Set<RaceTemplate>>() {});
      log.info("{}", objectMapper.writeValueAsString(list));
      for (RaceTemplate race : list) {
        templateRepo.registerRace(race);
      }
      //
    } catch (IOException e) {
      log.error("Error reading resources race", e);
    }

  }

  private void loadgSkillData() {
    log.info("loadgSkillData");

    try {
      // 使用 getResources (複數) 來支援萬用字元 *
      // Resource[] resources =
      // resourceResolver.getResources("classpath:data/global/skills/**/*.json");
      Resource[] resources =
          resourceResolver.getResources("classpath:data/global/skills/test/*.json");
      if (resources == null || resources.length == 0) {
        log.error("skill files not found");
        return;
      }
      for (Resource res : resources) {
        // 使用 try-with-resources 確保串流正確關閉
        try (var is = res.getInputStream()) {
          SkillTemplate tpl = objectMapper.readValue(is, SkillTemplate.class);
          log.info("Successfully loaded skill: {}:{}", tpl.getId(), tpl.getName());
          log.info("log:{}", objectMapper.writeValueAsString(tpl));
          templateRepo.registerSkill(tpl);
        } catch (Exception e) {
          log.error("Failed to parse JSON file: {} - Error: {}", res.getFilename(), e.getMessage());
        }
      }
    } catch (IOException e) {
      log.error("Error reading resources skills", e);
    }
  }


  private void readZone(String zoneId) {
    try {

      // 讀取 zone 資料
      Resource resource =
          resourceResolver.getResource("classpath:data/zones/" + zoneId + "/manifest.json");
      if (resource == null) {
        log.error("Zone manifest not found: {}", zoneId);
        return;
      }
      ZoneTemplate zoneTemplate =
          objectMapper.readValue(resource.getInputStream(), ZoneTemplate.class);
      // log.info("log:{}", objectMapper.writeValueAsString(zoneTemplate));
      templateRepo.registerZone(zoneTemplate);
      // String zoneId = zoneTemplate.id();


      // 讀取 mob 資料
      resource = resourceResolver.getResource("classpath:data/zones/" + zoneId + "/mobs.json");
      if (resource == null) {
        log.error("Zone mobs not found: {}", zoneId);
        return;
      }
      Set<MobTemplate> mobs = objectMapper.readValue(resource.getInputStream(),
          new TypeReference<Set<MobTemplate>>() {});
      for (MobTemplate mob : mobs) {
        String newMobId = IdUtils.resolveId(zoneId, mob.id());
        MobTemplate newMob = mob.toBuilder().id(newMobId).build();
        // log.info("log:{}", objectMapper.writeValueAsString(newMob));
        templateRepo.registerMob(newMob);
      }


      // 讀取 item 資料
      resource = resourceResolver.getResource("classpath:data/zones/" + zoneId + "/items.json");
      if (resource == null) {
        log.error("Zone items not found: {}", zoneId);
        return;
      }
      Set<ItemTemplate> items = objectMapper.readValue(resource.getInputStream(),
          new TypeReference<Set<ItemTemplate>>() {});
      for (ItemTemplate item : items) {
        String newItemId = IdUtils.resolveId(zoneId, item.id());
        ItemTemplate newItem = item.toBuilder().id(newItemId).build();
        // log.info("log:{}", objectMapper.writeValueAsString(newItem));
        templateRepo.registerItem(newItem);
      }


      // 讀取 room 資料
      resource = resourceResolver.getResource("classpath:data/zones/" + zoneId + "/rooms.json");
      if (resource == null) {
        log.error("Zone rooms not found: {}", zoneId);
        return;
      }

      // 使用 TypeReference 來正確讀取 JSON 陣列為 Collection<RoomTemplate>
      List<RoomTemplate> rooms = objectMapper.readValue(resource.getInputStream(),
          new TypeReference<List<RoomTemplate>>() {});

      for (RoomTemplate room : rooms) {
        // 1. 處理 Mob Spawn Rules (使用 Stream 避免 ConcurrentModificationException)
        List<SpawnRule> spawnRules = room.spawnRules() == null ? null
            : room.spawnRules().stream()
                .map(rule -> rule.toBuilder().id(IdUtils.resolveId(zoneId, rule.id())).build())
                .toList();

        // 3. 處理 Exits
        Map<String, RoomExit> updatedExits = room.exits() == null ? null
            : room.exits().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toBuilder()
                    .targetRoomId(IdUtils.resolveId(zoneId, e.getValue().targetRoomId())).build()));

        // 更新 room.id
        String newRoomId = IdUtils.resolveId(zoneId, room.id());
        RoomTemplate newRoom = room.toBuilder().id(newRoomId).zoneId(zoneId).exits(updatedExits)
            .spawnRules(spawnRules).build();
        // log.info("log:{}", objectMapper.writeValueAsString(newRoom));
        templateRepo.registerRoom(newRoom);

        // init roomActor
        getRoomActor(newRoomId);
      }

    } catch (Exception e) {
      log.error("read zone:{} rooms resources failed", zoneId, e);
    }
  }

  private void loadZone(String zoneId) {
    log.info("loadZone zoneId: {}", zoneId);
    templateRepo.getRoomTemplates().values().forEach(room -> {
      if (room.zoneId().equals(zoneId)) {
        getRoomActor(room.id());
      }
    });
  }

  /**
   * 核心方法：取得或創建 RoomActor 這是進入遊戲世界的入口
   */
  public Room getRoomActor(String roomId) {
    // 如果 Actor 已經存在，直接回傳
    return activeRooms.computeIfAbsent(roomId, id -> {
      return worldFactory.createRoom(id);
    });
  }

  public MobTemplate getMobTemplate(String mobId) {
    return templateRepo.findMob(mobId).orElseThrow(() -> {
      log.error("MobTemplate ID not found: " + mobId);
      // return new IllegalArgumentException("MobTemplate ID not found: " + mobId);
      return null;
    });
  }

  // ==========================================
  // Write-Behind Implementation (非同步存檔機制)
  // ==========================================

  // 定義存檔請求的封包 (Record)
  public record RoomStateUpdate(int roomId, String dataToSave) {
  }

  public void addLivingActor(Living living) {
    activeLivings.put(living.getId(), living);
  }

  public Optional<Living> findLivingActor(String livingId) {
    return Optional.ofNullable(activeLivings.get(livingId));
  }

  public Optional<Player> findPlayerActor(String playerId) {
    Optional<Living> living = findLivingActor(playerId);
    if (living.isPresent() && living.get() instanceof Player) {
      return Optional.of((Player) living.get());
    }
    return Optional.empty();
  }

  public void removeLivingActor(String livingId) {
    // 1. 直接從 Map 移除並取得物件，減少一次查詢
    Living living = activeLivings.remove(livingId);
    if (living == null) {
      return;
    }

    // 2. 如果是 Mob，確保停止其 Actor 訊息處理迴圈
    if (living instanceof Mob) {
      living.stop();
    }

    // 3. 委派給 Living 處理從房間移除的邏輯 (封裝職責)
    living.removeFromRoom();
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
      log.info("[Write-Behind] Saved room {} state to DB: {}", update.roomId(),
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
   * 建立一個全新的物品實體
   *
   * @param templateId 物品原型 ID
   * @param randomize 是否進行隨機數值浮動
   */
  // public GameItem createItem(int templateId, boolean randomize) {
  // ItemTemplate tpl = loadItemTemplate(templateId);

  // GameItem item = new GameItem();
  // item.setId(UUID.randomUUID().toString());
  // item.setTemplateId(tpl.id());
  // item.setCurrentDurability(tpl.maxDurability());
  // item.setAmount(1);

  // // --- 處理隨機邏輯 (RNG) ---
  // if (randomize) {
  // // 1. 隨機浮動耐久度 (例如：全新 ~ 80% 新)
  // // item.setCurrentDurability(...);

  // // 2. 隨機詞綴 (例如 10% 機率出現 +1 攻擊)
  // if (ThreadLocalRandom.current().nextDouble() < 0.1) {
  // item.getDynamicProps().put("attack_bonus", 1);
  // item.getDynamicProps().put("quality", "RARE");
  // }
  // }

  // return item;
  // }

  // public ItemTemplate loadItemTemplate(int templateId) {
  // // 1. DB -> Entity
  // ItemTemplateEntity entity = itemTemplateRepository.findById(templateId).orElseThrow(
  // () -> new IllegalArgumentException("ItemTemplate ID not found: " + templateId));

  // // 2. Entity -> Record (MapStruct 自動轉)
  // // 注意：這裡得到的 Record 內含的 State 是 Entity 裡解序列化出來的
  // return itemTemplateMapper.toRecord(entity);
  // }

  public Optional<Player> findPlayerByName(String targetName) {
    return activeLivings.values().stream().filter(Player.class::isInstance).map(Player.class::cast)
        .filter(player -> player.getName().equalsIgnoreCase(targetName)).findFirst();
  }
}
