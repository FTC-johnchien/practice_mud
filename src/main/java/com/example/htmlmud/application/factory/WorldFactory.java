package com.example.htmlmud.application.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.service.MobService;
import com.example.htmlmud.application.service.RoomService;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.ItemType;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.LootEntry;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.infra.mapper.ItemTemplateMapper;
import com.example.htmlmud.infra.mapper.MobMapper;
import com.example.htmlmud.infra.persistence.entity.SkillEntry;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldFactory {

  private final TemplateRepository templateRepo;

  private final MobMapper mobMapper;

  private final ItemTemplateMapper itemTemplateMapper;


  private final MobService mobService;

  private final ObjectProvider<RoomService> roomServiceProvider;

  /**
   * 建立房間 Actor
   */
  public Room createRoom(String roomId) {
    log.info("createRoom roomId: {}", roomId);

    // 這裡負責組裝：RoomActor
    Room room = new Room(roomId, roomServiceProvider.getObject());
    room.start();
    return room;
  }

  /**
   * 建立怪物 Actor (包含 AI 啟動邏輯)
   */
  public Mob createMob(String templateId) {
    // 1. 查 Template (Record)
    // log.info("createMob templateId: {}", templateId);
    MobTemplate tpl = templateRepo.findMob(templateId).orElse(null);
    if (tpl == null) {
      log.error("createMob failed: MobTemplate ID not found: " + templateId);
      throw new MudException("找不到這個怪物模板 MobTemplate ID: " + templateId);
    }

    // 2. new Actor
    LivingState state = mobMapper.toLivingState(tpl);
    Mob mob = new Mob(tpl, state, mobService);

    // log.info("{}", tpl.equipment());
    // 處理裝備
    for (var entry : tpl.equipment().entrySet()) {
      GameItem item = createItem(entry.getValue());
      if (item != null) {
        mob.getInventory().add(item);
        mob.getLivingService().equip(mob, item);

      }
    }

    // 處理技能
    // log.info("{}", tpl.enabledSkills());
    // 檢查是否有設定 enable 的技能
    if (tpl.enabledSkills() != null) {
      for (var entry : tpl.enabledSkills().entrySet()) {
        // 設定 mob 已學習的技能
        mob.getLearnedSkills().put(entry.getValue(),
            SkillEntry.createMobSkillEntry(entry.getValue(), mob.getLevel()));
        // 設定 mob 已啟用的技能
        mob.getEnabledSkills().put(entry.getKey(), entry.getValue());
      }
    }



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
    log.info("Item templateId:{}", templateId);
    ItemTemplate tpl = templateRepo.findItem(templateId).orElse(null);
    if (tpl == null) {
      log.error("Create Item failed: Template not found {}", templateId);
      return null;
    }

    // 使用 Mapper 進行轉換 (會自動複製 name, description, type, level 以及處理耐久度)
    GameItem item = itemTemplateMapper.toGameItem(tpl);

    item.setId(UUID.randomUUID().toString()); // 生成唯一 ID
    item.setAmount(1);

    // --- 處理隨機屬性 (RNG) ---
    // 這是 Factory 最有價值的地方，不要讓 Manager 變髒
    // if (tpl.chance() < 1.0) {
    // 處理掉落率/開箱率邏輯...
    // }

    // 範例：10% 機率出現稀有屬性
    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
      item.getDynamicProps().put("quality", "RARE");
      item.getDynamicProps().put("attack_bonus", 5);
    }

    return item;
  }

  public GameItem createCorpse(Living actor) {
    GameItem corpse = new GameItem();
    corpse.setId(UUID.randomUUID().toString());
    corpse.setName(actor.getName() + " 的屍體");
    corpse.setDescription("這裡有一具 " + actor.getName() + " 的屍體，死狀悽慘。");
    corpse.setType(ItemType.CORPSE);


    // 1. 【轉移遺物】：把怪物原本背包裡的東西移進去
    corpse.getContents().addAll(actor.getInventory());

    // 2. 【轉移裝備】：把怪物身上穿的脫下來移進去
    corpse.getContents().addAll(actor.getState().equipment.values());

    // 設定關鍵字，讓玩家可以用 "get corpse" 或 "get rat" 操作
    // 繼承怪物的關鍵字，再加上 "corpse"
    switch (actor) {
      case Player player:
        createPlayerCorpse(player, corpse);
        break;

      case Mob mob:
        createMobCorpse(mob, corpse);
        break;
    }

    return corpse;
  }

  private void createPlayerCorpse(Player player, GameItem corpse) {
    List<String> keywords = new ArrayList<>(player.getAliases());
    keywords.add("corpse");
  }

  private void createMobCorpse(Mob mob, GameItem corpse) {
    List<String> keywords = new ArrayList<>(mob.getTemplate().aliases());
    keywords.add("corpse");
    // corpse.setKeywords(keywords); // 假設您有 keywords 欄位

    // 3. 【產生掉落物】：根據 LootTable 骰骰子
    List<LootEntry> lootTable = mob.getTemplate().loot();
    if (lootTable != null) {
      for (LootEntry entry : lootTable) {
        if (ThreadLocalRandom.current().nextDouble() < entry.chance()) {
          GameItem loot = createItem(entry.itemId()); // 呼叫既有的 createItem
          if (loot != null) {
            corpse.addContent(loot);
          }
        }
      }
    }
  }

}
