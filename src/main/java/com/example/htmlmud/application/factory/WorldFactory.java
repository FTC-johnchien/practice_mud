package com.example.htmlmud.application.factory;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.domain.model.map.ZoneTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldFactory {

  // private final GameServices gameServices;

  // private final WorldManager worldManager;
  private final ScheduledExecutorService scheduler;

  private final TemplateRepository templateRepo;

  /**
   * 建立房間 Actor
   */
  public RoomActor createRoom(String roomId) {
    RoomTemplate roomTpl = templateRepo.findRoom(roomId).orElse(null);
    if (roomTpl == null) {
      log.error("Create Room failed: RoomTemplate not found {}", roomId);
      return null;
    }

    ZoneTemplate zoneTpl = templateRepo.findZone(roomTpl.zoneId()).orElse(null);
    if (zoneTpl == null) {
      log.error("Create Room failed: ZoneTemplate not found {}", roomTpl.zoneId());
      return null;
    }

    // 這裡負責組裝：RoomActor 需要 Template + ZoneTemplate
    return new RoomActor(roomTpl, zoneTpl, this);
  }

  /**
   * 建立怪物 Actor (包含 AI 啟動邏輯)
   */
  public MobActor createMob(String templateId) {
    // 1. 查 Template (Record)
    log.info("createMob templateId: {}", templateId);
    MobTemplate tpl = templateRepo.findMob(templateId).orElse(null);
    if (tpl == null) {
      log.error("MobTemplate ID not found: " + templateId);
      return null;
    }

    // 2. new Actor (State 自動生成)
    // MobActor mob = new MobActor(tpl);
    MobActor mob = new MobActor(tpl, scheduler);

    // 3. 這裡可以處理「菁英怪」或「隨機稱號」邏輯
    // if (Math.random() < 0.1) mob.setPrefix("狂暴的");

    // 4. 啟動 AI (重要：Factory 負責讓它動起來，或者由 Manager 統一啟動)
    mob.start();

    return mob;
  }

  /**
   * 建立物品實體 (處理隨機數值)
   */
  public GameItem createItem(String templateId) {
    ItemTemplate tpl = templateRepo.findItem(templateId).orElse(null);
    if (tpl == null) {
      log.error("Create Item failed: Template not found {}", templateId);
      return null;
    }

    GameItem item = new GameItem();
    item.setId(UUID.randomUUID().toString()); // 生成唯一 ID
    item.setTemplateId(tpl.id());
    item.setCurrentDurability(tpl.maxDurability());
    item.setAmount(1);

    // --- 處理隨機屬性 (RNG) ---
    // 這是 Factory 最有價值的地方，不要讓 Manager 變髒
    if (tpl.chance() < 1.0) {
      // 處理掉落率邏輯...
    }

    // 範例：10% 機率出現稀有屬性
    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
      item.getDynamicProps().put("quality", "RARE");
      item.getDynamicProps().put("attack_bonus", 5);
    }

    return item;
  }
}
