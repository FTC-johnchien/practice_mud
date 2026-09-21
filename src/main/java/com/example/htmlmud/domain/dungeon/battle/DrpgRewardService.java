package com.example.htmlmud.domain.dungeon.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.event.MobEvents;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.SkillBridgeService;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.example.htmlmud.domain.service.XpProgressionService;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 DRPG 戰鬥勝負結算、經驗發放、血肉道核剖取、首領擊殺事件發布、戰利品生成與戰後除名清理。
 */
@Slf4j
@Service
public class DrpgRewardService {

  private static final List<String> TOMB_ITEMS = List.of(
      "taiyin_pill", "purify_talisman", "bronze_sword", "yin_robe", "ancient_relic", "tomb_key"
  );

  private final TemplateReader templateReader;
  private final DungeonManager dungeonManager;

  @Autowired(required = false)
  private ApplicationEventPublisher eventPublisher;

  @Autowired(required = false)
  private XpProgressionService xpProgressionService;

  @Autowired(required = false)
  private CharacterSyncService characterSyncService;

  @Autowired(required = false)
  private SkillBridgeService skillBridgeService;

  @Autowired
  public DrpgRewardService(TemplateReader templateReader, DungeonManager dungeonManager) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
    this.dungeonManager = dungeonManager;
  }

  public DrpgRewardService() {
    this(new TemplateCatalog(), null);
  }

  public XpProgressionService getXpProgressionService() {
    if (xpProgressionService == null) {
      xpProgressionService = new XpProgressionService();
    }
    return xpProgressionService;
  }

  public void setXpProgressionService(XpProgressionService xpProgressionService) {
    this.xpProgressionService = xpProgressionService;
  }

  public CharacterSyncService getCharacterSyncService() {
    if (characterSyncService == null) {
      characterSyncService = new CharacterSyncService();
    }
    return characterSyncService;
  }

  public void setCharacterSyncService(CharacterSyncService characterSyncService) {
    this.characterSyncService = characterSyncService;
  }

  public SkillBridgeService getSkillBridgeService() {
    if (skillBridgeService == null) {
      skillBridgeService = new SkillBridgeService();
    }
    return skillBridgeService;
  }

  public void setSkillBridgeService(SkillBridgeService skillBridgeService) {
    this.skillBridgeService = skillBridgeService;
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * 結算戰鬥大捷
   */
  public void resolveVictory(Player player, BattleContext ctx, DungeonPosition pos,
      Map<String, BattleContext> activeBattles, Consumer<Player> stateBroadcaster) {
    if (player != null && activeBattles != null) {
      if (player.getName() != null) activeBattles.remove(player.getName());
      if (player.getId() != null) activeBattles.remove(player.getId());
    }

    int totalXp = ctx.getEnemies().stream().mapToInt(BattleEnemy::getXp).sum();
    if (totalXp <= 0) {
      totalXp = Math.max(30, ctx.getEnemies().size() * 30);
    }

    // 重置戰鬥專用資源
    for (PartyMember m : ctx.getParty().getMembers()) {
      m.setCurrentRage(0);
      m.setCurrentCombo(0);
    }

    // 發放戰鬥修為與判定升級
    List<String> levelUpAnnouncements = new ArrayList<>();
    var xpService = getXpProgressionService();
    for (PartyMember m : ctx.getParty().getMembers()) {
      if (m.isAlive() && m.getMadnessState() != PartyMember.MadnessState.DEAD_MEAT) {
        var res = xpService.awardExp(m, totalXp);
        if (res.isLeveledUp()) {
          levelUpAnnouncements.add(res.formatAnnouncement());
        }
      }
    }

    // 同步主角實體屬性 (透過 CharacterSyncService 維護單一真相源)
    if (player != null && ctx.getParty() != null && ctx.getParty().getLeader() != null) {
      getCharacterSyncService().syncFromPartyToPlayer(ctx.getParty().getLeader(), player);
      // 技能橋接回饋熟練度
      getSkillBridgeService().awardCombatSkillXp(player, totalXp);
    }

    List<String> droppedItemNames = new ArrayList<>();

    // 1. 檢查是否有被擊殺的深淵畸變體，掉落【血肉道核】
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive() && e.getId().startsWith("aberration-") && e.getDroppedDaoSkillId() != null) {
        String coreSlotId = "slot-" + UUID.randomUUID().toString().substring(0, 8);
        PartyItemSlot coreSlot = PartyItemSlot.builder()
            .slotId(coreSlotId)
            .itemId("dao_core_" + e.getDroppedDaoSkillId())
            .name("【血肉道核・" + e.getDroppedDaoMemberName() + "】")
            .icon("🧿")
            .itemType(ItemType.CONSUMABLE)
            .subType("SKILL_CORE")
            .count(1)
            .quality("EPIC")
            .description("自異變同伴殘軀中剖出的古神血肉道核，隱隱傳出殘留的道法共鳴。使用後可領悟其生前絕學【" + e.getDroppedDaoSkillName() + "】！")
            .effectType("LEARN_SKILL")
            .grantedSkillId(e.getDroppedDaoSkillId())
            .grantedSkillName(e.getDroppedDaoSkillName())
            .build();
        boolean added = ctx.getParty().getInventory().addSlot(coreSlot);
        if (added) {
          droppedItemNames.add("【血肉道核・" + e.getDroppedDaoMemberName() + "】(遺留絕學: " + e.getDroppedDaoSkillName() + ")");
        }
      }
    }

    // 2. 檢查首領擊殺並發布 Spring Event (觸發 MozhuMinesQuestListener 等任務監聽器)
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive() && e.getTemplateId() != null && e.getTemplateId().contains("boss_song_tianheng")) {
        if (eventPublisher != null) {
          try {
            eventPublisher.publishEvent(new MobEvents.MobDead(
                e.getTemplateId(),
                player != null ? player.getName() : "隊伍",
                Map.of(),
                Instant.now()
            ));
          } catch (Exception ex) {
            log.error("Failed to publish boss defeat event", ex);
          }
        }
        ctx.getParty().getInventory().addItem("mozhu_mines:black_obsidian_scythe", 1);
        ctx.getParty().getInventory().addItem("mozhu_mines:elder_token", 1);
        droppedItemNames.add("【黑曜骨鐮】");
        droppedItemNames.add("【長老黑話玉牌】");
      }
    }

    // 3. 普通戰利品掉落入背包
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive() && e.getDropItemId() != null && (e.getTemplateId() == null || !e.getTemplateId().contains("boss_song_tianheng"))) {
        String dropItemId = e.getDropItemId();
        var opt = templateReader.findItem(dropItemId);
        String dropName = opt.map(ItemTemplate::name).orElse(dropItemId);
        boolean added = ctx.getParty().getInventory().addItem(dropItemId, 1);
        if (added) {
          droppedItemNames.add(dropName);
        }
      }
    }
    if (droppedItemNames.isEmpty()) {
      String fallbackId = TOMB_ITEMS.get(ThreadLocalRandom.current().nextInt(TOMB_ITEMS.size()));
      var opt = templateReader.findItem(fallbackId);
      String dropName = opt.map(ItemTemplate::name).orElse("靈石碎片");
      if (ctx.getParty().getInventory().addItem(fallbackId, 1)) {
        droppedItemNames.add(dropName);
      }
    }

    // 4. 戰後清理 (解開封印 / 移除永久死肉與異變者)
    postBattleCleanup(player, ctx);

    String lootSummary = droppedItemNames.isEmpty() ? "（行囊已滿，未能裝入物品）" : String.join("、", droppedItemNames);

    StringBuilder victorySb = new StringBuilder();
    victorySb.append("\n\u001B[1;32m═══════════════════【戰鬥大捷】═══════════════════\n");
    victorySb.append("  小隊合力斬除太陰妖邪！全員獲得修為 ").append(totalXp).append(" 點！\n");
    for (String ann : levelUpAnnouncements) {
      victorySb.append("  \u001B[1;33m").append(ann).append("\u001B[1;32m\n");
    }
    victorySb.append("  戰利品已收納入隊伍行囊：【").append(lootSummary).append("】！\n");
    victorySb.append("══════════════════════════════════════════════════\u001B[0m\n");
    broadcastLog(player, ctx, victorySb.toString());

    if (stateBroadcaster != null && player != null) {
      stateBroadcaster.accept(player);
    }
  }

  /**
   * 結算戰鬥潰敗
   */
  public void resolveDefeat(Player player, BattleContext ctx, DungeonPosition pos,
      Map<String, BattleContext> activeBattles, Consumer<Player> stateBroadcaster) {
    if (player != null && activeBattles != null) {
      if (player.getName() != null) activeBattles.remove(player.getName());
      if (player.getId() != null) activeBattles.remove(player.getId());
    }

    // 扣除 SAN 並救治至 30% HP
    for (PartyMember m : ctx.getParty().getMembers()) {
      m.consumeSan(20);
      m.setCurrentRage(0);
      m.setCurrentCombo(0);
      if (m.getStats() != null) {
        m.getStats().setHp(Math.max(20, (int) (m.getStats().getMaxHp() * 0.3)));
        m.setAlive(true);
      }
    }

    postBattleCleanup(player, ctx);

    // 傳送回當前樓層起始入口
    if (pos != null) {
      if (pos.getFloorId() != null && dungeonManager != null) {
        DungeonFloor floor = dungeonManager.getFloor(pos.getFloorId());
        if (floor != null && floor.getStartCoord() != null) {
          pos.setCoord(floor.getStartCoord().x(), floor.getStartCoord().y());
        } else {
          pos.setCoord(1, 1);
        }
      } else {
        pos.setCoord(1, 1);
      }
    }

    String defeatMsg = "\n\u001B[1;31m═══════════════════【小隊潰敗】═══════════════════\n"
        + "  妖邪陰煞入體，小隊不敵！於千鈞一髮之際激發護身靈符遁回墓塚入口！\n"
        + "  全員身受重傷，道心嚴重受挫 (-20 SAN)！\n"
        + "══════════════════════════════════════════════════\u001B[0m\n";
    broadcastLog(player, ctx, defeatMsg);

    if (stateBroadcaster != null && player != null) {
      stateBroadcaster.accept(player);
    }
  }

  /**
   * 戰後狀態清理 (解開封印 / 清除死肉與異變同伴)
   */
  public void postBattleCleanup(Player player, BattleContext ctx) {
    if (ctx == null || ctx.getParty() == null) return;
    Party party = ctx.getParty();

    // 1. 解開封印 (恢復正常，SAN +20，計數器歸零)
    for (PartyMember m : party.getMembers()) {
      if (m.getMadnessState() == PartyMember.MadnessState.SEALED) {
        m.setMadnessState(PartyMember.MadnessState.SANE);
        m.setAberrationCounter(0);
        m.restoreSan(20);
        broadcastLog(player, ctx, "\u001B[1;34m🔓【鎮魔解印】戰鬥平息，解開 " + m.getName() + " 的鎖神符！煞氣受清氣沖刷漸漸退去，道心重獲安寧 (+20 SAN，當前: " + m.getCurrentSan() + ")，異變計數清零！\u001B[0m");
      }
    }

    // 2. 清理永久陣亡者 (DEAD_MEAT 或 ABERRATION)
    List<PartyMember> deadOrAberrant = party.getMembers().stream()
        .filter(m -> m.getMadnessState() == PartyMember.MadnessState.DEAD_MEAT || m.getMadnessState() == PartyMember.MadnessState.ABERRATION)
        .toList();
    for (PartyMember m : deadOrAberrant) {
      broadcastLog(player, ctx, "\u001B[1;31m💀【小隊除名】" + m.getName() + " 已身消道殞或墮入域外深淵，徹底除名於小隊編制！\u001B[0m");
    }
    party.getMembers().removeIf(m -> m.getMadnessState() == PartyMember.MadnessState.DEAD_MEAT || m.getMadnessState() == PartyMember.MadnessState.ABERRATION);
  }

  private void broadcastLog(Player player, BattleContext ctx, String msg) {
    if (ctx != null) {
      ctx.addLog(msg);
    }
    if (player != null && player.isValid()) {
      player.reply(msg);
    }
  }
}
