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
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.example.htmlmud.domain.service.XpProgressionService;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 DRPG 戰鬥勝負結算、經驗發放、血肉道核剖取、首領擊殺事件發布、戰利品生成與戰後除名清理。
 */
@Slf4j
@Service
public class DrpgRewardService {

  private final TemplateReader templateReader;
  private final DungeonManager dungeonManager;
  private ApplicationEventPublisher eventPublisher;
  private XpProgressionService xpProgressionService;
  private CharacterSyncService characterSyncService;
  private final com.example.htmlmud.domain.factory.ItemFactory itemFactory;

  @Autowired
  public DrpgRewardService(
      TemplateReader templateReader,
      DungeonManager dungeonManager,
      @Autowired(required = false) ApplicationEventPublisher eventPublisher,
      @Autowired(required = false) XpProgressionService xpProgressionService,
      @Autowired(required = false) CharacterSyncService characterSyncService,
      @Autowired(required = false) com.example.htmlmud.domain.factory.ItemFactory itemFactory) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
    this.dungeonManager = dungeonManager;
    this.eventPublisher = eventPublisher;
    this.xpProgressionService = xpProgressionService != null ? xpProgressionService : new XpProgressionService(this.templateReader);
    this.characterSyncService = characterSyncService != null ? characterSyncService : new CharacterSyncService();
    this.itemFactory = itemFactory != null ? itemFactory : new com.example.htmlmud.domain.factory.ItemFactory(this.templateReader);
  }

  public DrpgRewardService(TemplateReader templateReader, DungeonManager dungeonManager) {
    this(templateReader, dungeonManager, null, null, null, null);
  }

  public DrpgRewardService() {
    this(null, null);
  }

  public XpProgressionService getXpProgressionService() {
    return xpProgressionService;
  }

  public CharacterSyncService getCharacterSyncService() {
    return characterSyncService;
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void setXpProgressionService(XpProgressionService xpProgressionService) {
    this.xpProgressionService = xpProgressionService;
  }

  public void setCharacterSyncService(CharacterSyncService characterSyncService) {
    this.characterSyncService = characterSyncService;
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
    List<com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto.MemberLevelUpDto> levelUpDtos = new ArrayList<>();
    var xpService = getXpProgressionService();
    for (PartyMember m : ctx.getParty().getMembers()) {
      if (m.isAlive() && m.getMadnessState() != PartyMember.MadnessState.DEAD_MEAT) {
        int oldLvl = m.getLevel();
        var res = xpService.awardExp(m, totalXp);
        if (res.isLeveledUp()) {
          levelUpAnnouncements.add(res.formatAnnouncement());
          levelUpDtos.add(new com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto.MemberLevelUpDto(
              m.getId() != null ? m.getId() : m.getName(),
              m.getName(),
              oldLvl,
              m.getLevel(),
              res.formatAnnouncement()
          ));
        }
      }
    }

    // 同步主角實體屬性 (透過 CharacterSyncService 維護單一真相源)
    if (player != null && ctx.getParty() != null && ctx.getParty().getLeader() != null) {
      getCharacterSyncService().syncFromPartyToPlayer(ctx.getParty().getLeader(), player);
    }

    // 大一統技能原地修煉熟練度增長 (SP-06 / SP-07)
    if (ctx.getParty() != null && totalXp > 0) {
      long skillGain = Math.max(5, totalXp / 4);
      for (PartyMember m : ctx.getParty().getAliveMembers()) {
        if (m != null && m.getSkills() != null) {
          for (com.example.htmlmud.domain.party.model.PartyMemberSkill s : m.getSkills()) {
            int oldLevel = s.getLevel();
            s.addXp(skillGain);
            if (s.getLevel() > oldLevel && player != null && player.isValid()) {
              player.reply("\u001B[1;36m💡【武學頓悟】" + m.getName() + " 在太陰死鬥中歷經磨礪，技能【" + s.getName() + "】突破精進至 Lv." + s.getLevel() + "！\u001B[0m");
            }
            if (player != null && m == ctx.getParty().getLeader() && player.getLearnedSkills() != null) {
              var entry = player.getLearnedSkills().get(s.getId());
              if (entry != null) {
                entry.setLevel(s.getLevel());
                entry.setXp(s.getXp());
                entry.setMaxXp(s.getMaxXp());
              }
            }
          }
        }
      }
    }

    List<String> droppedItemNames = new ArrayList<>();
    List<com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto.BattleLootDto> lootDtos = new ArrayList<>();

    // 1. 檢查是否有被擊殺的深淵畸變體，掉落【血肉道核】
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive() && e.getId() != null && e.getId().startsWith("aberration-") && e.getDroppedDaoSkillId() != null) {
        PartyItemSlot coreSlot = itemFactory.createDaoCore(
            e.getDroppedDaoMemberName(), e.getDroppedDaoSkillId(), e.getDroppedDaoSkillName());
        boolean added = ctx.getParty().getInventory().addSlot(coreSlot);
        if (added) {
          droppedItemNames.add(coreSlot.getName() + "(遺留絕學: " + e.getDroppedDaoSkillName() + ")");
          lootDtos.add(new com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto.BattleLootDto(
              coreSlot.getItemId(), coreSlot.getName(), coreSlot.getIcon(), coreSlot.getQuality(),
              coreSlot.getCount(), coreSlot.getDescription()
          ));
          if (eventPublisher != null) {
            try {
              eventPublisher.publishEvent(new com.example.htmlmud.domain.event.PlayerEvents.ItemAcquired(
                  player != null ? player.getName() : "隊伍",
                  coreSlot.getItemId(),
                  coreSlot.getCount()
              ));
            } catch (Exception ex) {
              log.error("Failed to publish ItemAcquired event for dao core", ex);
            }
          }
        }
      }
    }

    // 2. 檢查被擊殺怪物並發布 Spring Event (觸發 MozhuMinesQuestListener 等任務監聽器)
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive() && e.getTemplateId() != null) {
        if (eventPublisher != null) {
          try {
            eventPublisher.publishEvent(new MobEvents.MobDead(
                e.getTemplateId(),
                player != null ? player.getName() : "隊伍",
                Map.of(),
                Instant.now()
            ));
          } catch (Exception ex) {
            log.error("Failed to publish mob dead event for {}", e.getTemplateId(), ex);
          }
        }
      }
    }

    // 3. 戰利品掉落入背包 (從被擊殺敵人的 dropItemIds 解析)
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive()) {
        for (String dropItemId : e.getDropItemIds()) {
          if (dropItemId == null || dropItemId.isBlank()) continue;
          var opt = templateReader.findItem(dropItemId);
          var def = opt.map(ItemTemplate::toDefinition).orElse(null);
          String dropName = def != null ? def.name() : dropItemId;
          String dropIcon = def != null && def.icon() != null ? def.icon() : "📦";
          String dropQuality = def != null && def.quality() != null ? def.quality() : "COMMON";
          String dropDesc = def != null && def.description() != null ? def.description() : "戰鬥中繳獲的戰利品。";
          boolean added = ctx.getParty().getInventory().addItem(dropItemId, 1);
          if (added) {
            droppedItemNames.add(dropName);
            lootDtos.add(new com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto.BattleLootDto(
                dropItemId, dropName, dropIcon, dropQuality, 1, dropDesc
            ));
            if (eventPublisher != null) {
              try {
                eventPublisher.publishEvent(new com.example.htmlmud.domain.event.PlayerEvents.ItemAcquired(
                    player != null ? player.getName() : "隊伍",
                    dropItemId,
                    1
                ));
              } catch (Exception ex) {
                log.error("Failed to publish ItemAcquired event for drop {}", dropItemId, ex);
              }
            }
          }
        }
      }
    }

    // 4. 計算繳獲靈石並入帳小隊金庫 (SP-09)
    int rewardCoin = 0;
    for (BattleEnemy e : ctx.getEnemies()) {
      if (!e.isAlive()) {
        int enemyCoin = 8;
        if (e.getRank() != null) {
          enemyCoin = switch (e.getRank()) {
            case NORMAL -> 8;
            case ELITE -> 25;
            case BOSS -> 80;
          };
        }
        rewardCoin += enemyCoin;
      }
    }
    if (rewardCoin > 0 && ctx.getParty() != null) {
      ctx.getParty().addCoin(rewardCoin);
      if (player != null && player.getStats() != null) {
        player.getStats().setCoin(ctx.getParty().getCoin());
      }
    }

    // 5. 戰後清理 (解開封印 / 移除永久死肉與異變者)
    postBattleCleanup(player, ctx);

    // 6. 推送結構化大捷事件給客戶端，用於渲染視覺化結算視窗
    if (player != null) {
      var victoryDto = new com.example.htmlmud.domain.dungeon.dto.BattleVictoryDto(
          ctx.getBattleId(), totalXp, rewardCoin, levelUpDtos, lootDtos
      );
      player.sendJson(victoryDto);
    }

    // 7. 文字日誌純粹單行化 (遵守第 10 條鐵律)
    String lootSummary = droppedItemNames.isEmpty() ? "無特殊靈物" : String.join("、", droppedItemNames);
    String coinSummary = (rewardCoin > 0) ? "，繳獲靈石 " + rewardCoin + " 枚（小隊金庫當前: " + (ctx.getParty() != null ? ctx.getParty().getCoin() : rewardCoin) + "）" : "";
    broadcastLog(player, ctx, "🏆 \u001B[1;32m【戰鬥大捷】小隊合力斬除妖邪！全員獲得修為 " + totalXp + " 點" + coinSummary + "，獲取戰利品：【" + lootSummary + "】！\u001B[0m");
    for (String ann : levelUpAnnouncements) {
      broadcastLog(player, ctx, "✨ \u001B[1;33m" + ann + "\u001B[0m");
    }

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
