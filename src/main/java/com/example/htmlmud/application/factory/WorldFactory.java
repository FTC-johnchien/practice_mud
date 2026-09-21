package com.example.htmlmud.application.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.config.LootEntry;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.MobService;
import com.example.htmlmud.domain.service.RoomService;
import com.example.htmlmud.infra.mapper.ItemTemplateMapper;
import com.example.htmlmud.infra.mapper.MobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldFactory {

  private final MobMapper mobMapper;

  private final ItemTemplateMapper itemTemplateMapper;


  private final MobService mobService;

  private final TemplateReader templateReader;

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
    MobTemplate tpl = templateReader.findMob(templateId).orElse(null);
    if (tpl == null) {
      log.error("createMob failed: MobTemplate ID not found: " + templateId);
      throw new MudException("找不到這個怪物模板 MobTemplate ID: " + templateId);
    }

    // 2. new Actor
    LivingStats stats = mobMapper.toLivingStats(tpl);
    Mob mob = new Mob(tpl, stats, mobService);
    mob.setTemplateReader(templateReader);
    mob.start();

    // log.info("{}", tpl.equipment());
    // 處理裝備
    for (var entry : tpl.equipment().entrySet()) {
      GameItem item = createItem(entry.getValue());
      if (item != null) {
        mob.getInventory().add(item);
        mob.equip(item);

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

    // 依據種族自動補齊天然防禦 (DODGE, PARRY) 與攻擊技能 (Race Natural Skills Binding)
    String raceId = tpl.race() != null ? tpl.race() : stats.getRace();
    if (raceId != null) {
      templateReader.findRace(raceId).ifPresent(raceTpl -> {
        if (raceTpl.combat() != null) {
          // 1. 天然身法閃避 (Dodge)
          if (!mob.getEnabledSkills().containsKey(SkillCategory.DODGE) && raceTpl.combat().naturalDodge() != null) {
            String dodgeSkill = raceTpl.combat().naturalDodge();
            mob.getLearnedSkills().put(dodgeSkill, SkillEntry.createMobSkillEntry(dodgeSkill, mob.getLevel()));
            mob.getEnabledSkills().put(SkillCategory.DODGE, dodgeSkill);
          }
          // 2. 天然兵刃/肢體招架 (Parry)
          if (!mob.getEnabledSkills().containsKey(SkillCategory.PARRY) && raceTpl.combat().naturalParry() != null) {
            String parrySkill = raceTpl.combat().naturalParry();
            mob.getLearnedSkills().put(parrySkill, SkillEntry.createMobSkillEntry(parrySkill, mob.getLevel()));
            mob.getEnabledSkills().put(SkillCategory.PARRY, parrySkill);
          }
          // 3. 天然攻擊招式 (Natural Attacks)
          if (raceTpl.combat().naturalAttacks() != null) {
            for (var atk : raceTpl.combat().naturalAttacks()) {
              if (atk.id() != null && !mob.getLearnedSkills().containsKey(atk.id())) {
                mob.getLearnedSkills().put(atk.id(), SkillEntry.createMobSkillEntry(atk.id(), mob.getLevel()));
              }
            }
          }
        }
      });
    }



    // 3. 這裡可以處理「菁英怪」或「隨機稱號」邏輯
    // if (Math.random() < 0.1) mob.setPrefix("狂暴的");

    return mob;
  }

  /**
   * 建立物品實體 (處理隨機數值)
   */
  public GameItem createItem(String templateId) {
    log.info("Item templateId:{}", templateId);
    ItemTemplate tpl = templateReader.findItem(templateId).orElse(null);
    if (tpl == null) {
      log.error("Create Item failed: Template not found {}", templateId);
      return null;
    }

    // 使用 Mapper 進行轉換 (會自動複製 name, description, type, level 以及處理耐久度)
    GameItem item = itemTemplateMapper.toGameItem(tpl);

    item.setId(UUID.randomUUID().toString()); // 生成唯一 ID
    // item.setName(templateId);
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

  public GameItem createCorpse(Living actor, String killerName) {
    GameItem corpse = new GameItem();
    corpse.setId(UUID.randomUUID().toString());
    corpse.setType(ItemType.CORPSE);
    if (killerName != null) {
      corpse.setName(killerName + " 殺死的" + actor.getName() + "的屍體");
    } else {
      corpse.setName(actor.getName() + "的屍體");
    }
    corpse.getAliases().add("corpse");
    corpse.getAliases().addAll(actor.getAliases());

    String desc = null;
    switch (actor) {
      case Player player:
        desc = "這裡有一具" + player.getNickname() + "的屍體，死狀悽慘。";
        createPlayerCorpse(player, corpse);
        break;

      case Mob mob:
        desc = mob.getTemplate().lookDescription();
        if (desc == null) {
          desc = mob.getTemplate().description();
        }
        createMobCorpse(mob, corpse);
        break;
    }
    corpse.setDescription(desc);

    return corpse;
  }

  private void createPlayerCorpse(Player player, GameItem corpse) {
    // List<String> keywords = new ArrayList<>(player.getAliases());
    // keywords.add("corpse");

    // 1. 【轉移遺物】：把玩家原本背包裡的東西移進去
    corpse.getContents().addAll(player.getInventory());

    // 2. 【轉移裝備】：把玩家身上穿的脫下來移進去
    corpse.getContents().addAll(player.getStats().equipment.values());
  }

  public List<GameItem> generateMobDrops(Mob mob) {
    List<GameItem> drops = new ArrayList<>();
    if (mob == null || mob.getTemplate() == null) return drops;

    List<LootEntry> lootTable = mob.getTemplate().loot();
    if (lootTable != null) {
      for (LootEntry entry : lootTable) {
        if (ThreadLocalRandom.current().nextDouble() < entry.chance()) {
          GameItem loot = createItem(entry.itemId());
          if (loot != null) {
            drops.add(loot);
          }
        }
      }
    }
    return drops;
  }

  public GameItem createLootPouch(Mob mob, List<GameItem> drops) {
    GameItem pouch = new GameItem();
    pouch.setId(UUID.randomUUID().toString());
    pouch.setType(ItemType.CONTAINER);

    boolean isBossOrElite = mob != null && mob.getTemplate() != null &&
        (mob.getTemplate().id().contains("boss") ||
         mob.getTemplate().id().contains("elite") ||
         mob.getName().contains("頭領") ||
         mob.getName().contains("長老") ||
         mob.getName().contains("大師兄"));

    if (isBossOrElite) {
      pouch.setName("【" + mob.getName() + "的戰利品寶箱】");
      pouch.setDescription("散發著幽幽靈光的秘寶箱，隱隱透出法寶神兵的氣息。");
      pouch.setAliases(new ArrayList<>(List.of("box", "chest", "treasure", "loot", "箱子", "寶箱")));
    } else {
      String mobName = (mob != null) ? mob.getName() : "妖獸";
      pouch.setName("【散落的儲物袋】");
      pouch.setDescription("一隻以粗麻縫製的低階儲物袋，裡面似乎裝著擊殺 " + mobName + " 後遺留下來的戰利品。");
      pouch.setAliases(new ArrayList<>(List.of("pouch", "bag", "loot", "儲物袋", "袋子")));
    }

    if (drops != null) {
      pouch.getContents().addAll(drops);
    }
    return pouch;
  }

  private void createMobCorpse(Mob mob, GameItem corpse) {
    List<GameItem> drops = generateMobDrops(mob);
    for (GameItem drop : drops) {
      corpse.addContent(drop);
    }
  }

}
