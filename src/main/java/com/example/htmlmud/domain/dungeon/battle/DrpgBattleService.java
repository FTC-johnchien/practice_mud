package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto;
import com.example.htmlmud.domain.dungeon.dto.BattleViewDto;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DrpgBattleService {
  private final PartyService partyService;
  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;

  private final Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();

  private static final List<String> TOMB_ITEMS = List.of(
      "taiyin_pill", "purify_talisman", "bronze_sword", "yin_robe", "ancient_relic", "tomb_key"
  );

  @org.springframework.beans.factory.annotation.Autowired
  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager, DungeonNavigator dungeonNavigator) {
    this.partyService = partyService;
    this.dungeonManager = dungeonManager;
    this.dungeonNavigator = dungeonNavigator != null ? dungeonNavigator : new DungeonNavigator();
  }

  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager) {
    this(partyService, dungeonManager, new DungeonNavigator());
  }

  public boolean isInBattle(String playerId) {
    BattleContext ctx = activeBattles.get(playerId);
    return ctx != null && !ctx.isOver();
  }

  public BattleContext getBattle(String playerId) {
    return activeBattles.get(playerId);
  }

  public void startBattle(Player player, DungeonPosition pos, List<String> mobTemplateIds) {
    Party party = partyService.getOrCreateParty(player.getName());

    List<BattleEnemy> enemies = new ArrayList<>();
    int idx = 0;
    for (String templateId : mobTemplateIds) {
      idx++;
      var opt = TemplateRepository.findMob(templateId);
      String name = opt.map(MobTemplate::name).orElse("太陰妖邪") + " " + (char)('A' + idx - 1);
      int maxHp = 70 + (idx * 20);
      RowPosition row = (templateId.contains("bat") || templateId.contains("mage")) ? RowPosition.BACK : RowPosition.FRONT;

      BattleEnemy enemy = BattleEnemy.builder()
          .id("mob-" + idx)
          .templateId(templateId)
          .name(name)
          .hp(maxHp)
          .maxHp(maxHp)
          .minDamage(10 + idx * 2)
          .maxDamage(18 + idx * 3)
          .defense(4 + idx)
          .dex(10)
          .row(row)
          .attackIntervalMs(2200 + ThreadLocalRandom.current().nextLong(-300, 400))
          .nextAttackTime(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(1200, 2200))
          .alive(true)
          .xp(35 + idx * 10)
          .dropItemId(TOMB_ITEMS.get(ThreadLocalRandom.current().nextInt(TOMB_ITEMS.size())))
          .build();
      enemies.add(enemy);
    }

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-" + player.getName() + "-" + System.currentTimeMillis())
        .playerId(player.getName())
        .party(party)
        .enemies(enemies)
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    // 隊員首次攻擊隨機錯開，製造錯落交鋒節奏
    long now = System.currentTimeMillis();
    for (PartyMember m : party.getMembers()) {
      m.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(600, 1600));
    }

    broadcastLog(player, ctx, "\n\u001B[1;31m⚔️【遭遇妖邪】墓道深處殺氣逼近，妖邪敵群列陣襲來！請迎戰！\u001B[0m");
    activeBattles.put(player.getName(), ctx);

    // 推送戰鬥開始 DRPG_STATE
    pushDrpgState(player, pos, ctx);

    // 啟動虛擬執行緒戰鬥心跳
    Thread.ofVirtual().name("BattleLoop-" + player.getName()).start(() -> {
      runBattleLoop(player, ctx, pos);
    });
  }

  private void runBattleLoop(Player player, BattleContext ctx, DungeonPosition pos) {
    try {
      while (!ctx.isOver() && player.isValid()) {
        long now = System.currentTimeMillis();

        // 1. 我方隊員自動普攻
        for (PartyMember member : ctx.getParty().getMembers()) {
          if (!member.isAlive() || ctx.isOver()) continue;

          if (now >= member.getNextAttackTime()) {
            member.setNextAttackTime(now + member.getAttackIntervalMs());

            BattleEnemy target = (member.getRow() == RowPosition.FRONT)
                ? ctx.getFrontTargetEnemy() : ctx.getTargetEnemy();

            if (target != null && target.isAlive()) {
              int dmg = calculatePlayerDamage(member, target);
              target.takeDamage(dmg);

              // 連擊點累積
              if (member.getResourceType() == ResourceType.COMBO) {
                member.gainCombo(1);
              }
              // 陣法靈威積累
              ctx.getParty().addFormationEnergy(2);

              broadcastLog(player, ctx, "🗡️ " + member.getName() + " 運勁平刺，擊中【" + target.getName() + "】造成 " + dmg + " 點傷害！");

              if (!target.isAlive()) {
                broadcastLog(player, ctx, "\u001B[1;32m💥【" + target.getName() + "】被斬殺倒地！\u001B[0m");
                if (ctx.isAllEnemiesDead()) {
                  ctx.setState(BattleState.VICTORY);
                  break;
                }
              }
            }
          }
        }

        if (ctx.isOver()) break;

        // 2. 敵方怪物自動攻擊
        for (BattleEnemy enemy : ctx.getEnemies()) {
          if (!enemy.isAlive() || enemy.isStunned() || ctx.isOver()) continue;

          if (now >= enemy.getNextAttackTime()) {
            enemy.setNextAttackTime(now + enemy.getAttackIntervalMs());

            PartyMember targetMember = selectPartyTarget(ctx);
            if (targetMember != null && targetMember.isAlive()) {
              int rawDmg = ThreadLocalRandom.current().nextInt(enemy.getMinDamage(), enemy.getMaxDamage() + 1);
              int finalDmg = Math.max(1, rawDmg - targetMember.getBaseDefense());
              targetMember.takeDamage(finalDmg);

              // 力士受傷累積怒氣 (+15)
              if (targetMember.getResourceType() == ResourceType.RAGE) {
                targetMember.gainRage(15);
              }

              broadcastLog(player, ctx, "\u001B[1;31m⚡【" + enemy.getName() + "】爪擊撕咬，重創 " + targetMember.getName() + " 造成 " + finalDmg + " 點傷害！\u001B[0m");

              if (!targetMember.isAlive()) {
                broadcastLog(player, ctx, "\u001B[1;35m💀 " + targetMember.getName() + " 力竭倒下！\u001B[0m");
                if (ctx.isAllPartyDead()) {
                  ctx.setState(BattleState.DEFEAT);
                  break;
                }
              }
            }
          }
        }

        // 推送戰鬥進度
        pushDrpgState(player, pos, ctx);

        Thread.sleep(500);
      }

      // 戰鬥結束結算
      if (ctx.getState() == BattleState.VICTORY) {
        resolveVictory(player, ctx, pos);
      } else if (ctx.getState() == BattleState.DEFEAT) {
        resolveDefeat(player, ctx, pos);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Battle loop error for player {}", player.getName(), e);
    } finally {
      activeBattles.remove(player.getName());
      pushDrpgState(player, pos, null);
    }
  }

  private int calculatePlayerDamage(PartyMember member, BattleEnemy target) {
    int base = ThreadLocalRandom.current().nextInt(member.getBaseMinDamage(), member.getBaseMaxDamage() + 1);
    int net = Math.max(1, base - target.getDefense());
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    return (int) (net * variance);
  }

  private PartyMember selectPartyTarget(BattleContext ctx) {
    // 若有嘲諷目標且活著，強制打嘲諷者
    if (ctx.isTaunted()) {
      for (PartyMember m : ctx.getParty().getMembers()) {
        if (m.getId().equals(ctx.getTauntedByMemberId()) && m.isAlive()) {
          return m;
        }
      }
    }

    // 優先挑選前排活著的隊員
    List<PartyMember> frontAlive = ctx.getParty().getMembers().stream()
        .filter(m -> m.isAlive() && m.getRow() == RowPosition.FRONT)
        .toList();
    if (!frontAlive.isEmpty()) {
      return frontAlive.get(ThreadLocalRandom.current().nextInt(frontAlive.size()));
    }

    // 前排無人，打後排
    List<PartyMember> backAlive = ctx.getParty().getMembers().stream()
        .filter(PartyMember::isAlive)
        .toList();
    if (!backAlive.isEmpty()) {
      return backAlive.get(ThreadLocalRandom.current().nextInt(backAlive.size()));
    }
    return null;
  }

  /**
   * 玩家手動點擊「迎戰」：全員氣勢如虹，並讓準備好的隊員即時出手
   */
  public void fight(Player player, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("【戰鬥】四周暫無妖邪現身，請移步探索。");
      return;
    }
    BattleEnemy target = ctx.getTargetEnemy();
    if (target != null && target.isAlive()) {
      broadcastLog(player, ctx, "\u001B[1;31m⚔️【迎戰】全隊拔劍出鞘，陣型收束，集中火力直攻【" + target.getName() + "】！\u001B[0m");
      // 推進我方攻速 CD，讓至少一名隊員立即發動攻擊
      long now = System.currentTimeMillis();
      for (PartyMember member : ctx.getParty().getMembers()) {
        if (member.isAlive() && member.getNextAttackTime() > now + 300) {
          member.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(50, 250));
        }
      }
    }
    pushDrpgState(player, pos, ctx);
  }

  public void castSkill(Player player, int memberIdx, String skillId, int targetIdx, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前並未處於戰鬥之中！");
      return;
    }

    if (memberIdx < 0 || memberIdx >= ctx.getParty().getMembers().size()) {
      player.reply("無效的隊員編號！");
      return;
    }

    PartyMember member = ctx.getParty().getMembers().get(memberIdx);
    if (!member.isAlive()) {
      player.reply(member.getName() + " 已經倒下，無法施展技能！");
      return;
    }

    PartyMemberSkill skill = member.getSkills().stream()
        .filter(s -> s.getId().equalsIgnoreCase(skillId))
        .findFirst().orElse(null);

    if (skill == null) {
      player.reply(member.getName() + " 並未掌握該項技能！");
      return;
    }

    if (member.isOnCooldown(skill.getId())) {
      long remainSec = (member.getRemainingCooldownMs(skill.getId()) / 1000) + 1;
      player.reply("招式冷卻中，尚需 " + remainSec + " 秒！");
      return;
    }

    // 資源扣除檢驗
    if (skill.getCostType() == ResourceType.MP) {
      if (!member.consumeMp(skill.getCostValue())) {
        player.reply("真元不足！需要 " + skill.getCostValue() + " MP！");
        return;
      }
    } else if (skill.getCostType() == ResourceType.RAGE) {
      if (!member.consumeRage(skill.getCostValue())) {
        player.reply("怒氣未滿！需要 " + skill.getCostValue() + " 點怒氣！");
        return;
      }
    } else if (skill.getCostType() == ResourceType.COMBO) {
      if (!member.consumeCombo(skill.getCostValue())) {
        player.reply("連擊點不足！需要 " + skill.getCostValue() + " 層連擊！");
        return;
      }
    }

    // 進入冷卻
    member.setCooldown(skill.getId(), skill.getCooldownMs());
    ctx.getParty().addFormationEnergy(8);

    // 執行技能效果
    if (skill.isHeal()) {
      if (skill.isAoe()) {
        for (PartyMember m : ctx.getParty().getMembers()) {
          if (m.isAlive()) {
            m.heal(skill.getHealAmount());
            if (skill.getSanRestore() > 0) m.restoreSan(skill.getSanRestore());
          }
        }
        broadcastLog(player, ctx, "\u001B[1;32m✨ " + member.getName() + " 施展【" + skill.getName() + "】，甘露靈泉籠罩全隊！氣血恢復，道心安穩！\u001B[0m");
      } else {
        // 單體補血，挑選血量比例最低的隊員
        PartyMember lowest = ctx.getParty().getMembers().stream()
            .filter(PartyMember::isAlive)
            .min((a, b) -> Integer.compare(a.getStats().getHp(), b.getStats().getHp()))
            .orElse(member);
        lowest.heal(skill.getHealAmount());
        broadcastLog(player, ctx, "\u001B[1;32m🌿 " + member.getName() + " 運轉【" + skill.getName() + "】，一道春生靈氣注入 " + lowest.getName() + "，恢復 " + skill.getHealAmount() + " 點氣血！\u001B[0m");
      }
    } else if (skill.isTaunt()) {
      ctx.setTaunt(member.getId(), 5000);
      broadcastLog(player, ctx, "\u001B[1;33m🛡️ " + member.getName() + " 爆發【" + skill.getName() + "】，金剛威儀震懾全場！所有怪物仇恨被強行吸引！\u001B[0m");
    } else {
      // 傷害技能
      if (skill.isAoe()) {
        for (BattleEnemy e : ctx.getEnemies()) {
          if (e.isAlive()) {
            int dmg = (int) (calculatePlayerDamage(member, e) * skill.getDamageMultiplier());
            e.takeDamage(dmg);
            if (skill.isStun()) e.applyStun(skill.getStunDurationSeconds() * 1000L);
            if (!e.isAlive()) broadcastLog(player, ctx, "\u001B[1;32m💥【" + e.getName() + "】在靈力轟擊下灰飛煙滅！\u001B[0m");
          }
        }
        broadcastLog(player, ctx, "\u001B[1;36m🌩️ " + member.getName() + " 祭出【" + skill.getName() + "】，排山倒海的威能橫掃敵方全體！\u001B[0m");
        if (ctx.isAllEnemiesDead()) ctx.setState(BattleState.VICTORY);
      } else {
        BattleEnemy target = ctx.getTargetEnemy();
        if (target != null && target.isAlive()) {
          int dmg = (int) (calculatePlayerDamage(member, target) * skill.getDamageMultiplier());
          target.takeDamage(dmg);
          if (skill.isStun()) target.applyStun(skill.getStunDurationSeconds() * 1000L);
          broadcastLog(player, ctx, "\u001B[1;33m🔥 " + member.getName() + " 施展【" + skill.getName() + "】，直取【" + target.getName() + "】要害，造成 " + dmg + " 點毀滅打擊！\u001B[0m");
          if (!target.isAlive()) {
            broadcastLog(player, ctx, "\u001B[1;32m💥【" + target.getName() + "】慘叫倒地氣絕！\u001B[0m");
            if (ctx.isAllEnemiesDead()) ctx.setState(BattleState.VICTORY);
          }
        }
      }
    }

    pushDrpgState(player, pos, ctx);
  }

  public void castPartyUltimate(Player player, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前未在戰鬥中！");
      return;
    }
    Party party = ctx.getParty();
    if (!party.canCastUltimate()) {
      player.reply("陣法靈威不足 100，無法施展陣法奧義！");
      return;
    }

    party.setFormationEnergy(0);
    String formId = party.getEquippedFormation() != null ? party.getEquippedFormation().getId() : "";

    if ("formation_xuan_yin".equals(formId)) {
      // 不可名狀星蝕
      for (PartyMember m : party.getMembers()) {
        m.consumeSan(8);
      }
      for (BattleEnemy e : ctx.getEnemies()) {
        if (e.isAlive()) {
          e.takeDamage(240);
        }
      }
      broadcastLog(player, ctx, "\u001B[1;35m⚡⚡【陣法奧義・不可名狀星蝕】虛空被撕裂成深邃盲目之眼！狂亂囈語灌入心神 (-8 SAN)！全體敵方遭受 240 點暗蝕滅頂之災！\u001B[0m");
    } else {
      // 四象辟邪聖光
      for (PartyMember m : party.getMembers()) {
        if (m.isAlive()) {
          m.heal(50);
          m.restoreSan(15);
        }
      }
      for (BattleEnemy e : ctx.getEnemies()) {
        if (e.isAlive()) {
          e.takeDamage(120);
        }
      }
      broadcastLog(player, ctx, "\u001B[1;33m⚡⚡【陣法奧義・四象辟邪聖光】青龍白虎朱雀玄武四聖法相齊現！聖光普照全員氣血大盛 (+50 HP, +15 SAN)，敵方遭受 120 點灼魂傷害！\u001B[0m");
    }

    if (ctx.isAllEnemiesDead()) {
      ctx.setState(BattleState.VICTORY);
    }
    pushDrpgState(player, pos, ctx);
  }

  public void selectTarget(Player player, int targetIdx, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx != null && targetIdx >= 0 && targetIdx < ctx.getEnemies().size()) {
      ctx.setSelectedTargetIndex(targetIdx);
      BattleEnemy e = ctx.getEnemies().get(targetIdx);
      broadcastLog(player, ctx, "🎯 全隊鎖定集火目標：【" + e.getName() + "】！");
      pushDrpgState(player, pos, ctx);
    }
  }

  public void flee(Player player, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前並未在戰鬥中！");
      return;
    }

    // 75% 成功率
    if (ThreadLocalRandom.current().nextInt(100) < 75) {
      for (PartyMember m : ctx.getParty().getMembers()) {
        m.consumeSan(5);
      }
      ctx.setState(BattleState.FLED);
      broadcastLog(player, ctx, "\u001B[1;33m🏃【遁地金光】小隊捏碎遁符，借陰風遁出百丈！代價道心微顫 (-5 SAN)！\u001B[0m");
      activeBattles.remove(player.getName());
      pushDrpgState(player, pos, null);
      player.reply("你成功擺脫了妖邪的追擊！");
    } else {
      broadcastLog(player, ctx, "\u001B[1;31m⚠️ 逃跑失敗！妖邪陰氣封鎖了退路！\u001B[0m");
      pushDrpgState(player, pos, ctx);
    }
  }

  private void resolveVictory(Player player, BattleContext ctx, DungeonPosition pos) {
    activeBattles.remove(player.getName());
    int totalXp = ctx.getEnemies().stream().mapToInt(BattleEnemy::getXp).sum();

    String dropItemId = TOMB_ITEMS.get(ThreadLocalRandom.current().nextInt(TOMB_ITEMS.size()));
    var opt = TemplateRepository.findItem(dropItemId);
    String dropName = opt.map(ItemTemplate::name).orElse("太陰靈物");

    // 重置戰鬥資源
    for (PartyMember m : ctx.getParty().getMembers()) {
      m.setCurrentRage(0);
      m.setCurrentCombo(0);
    }

    String victoryMsg = "\n\u001B[1;32m═══════════════════【戰鬥大捷】═══════════════════\n"
        + "  小隊合力斬除太陰妖邪！全員獲得修為 " + totalXp + " 點！\n"
        + "  自妖邪殘骸中尋得戰利品：【" + dropName + "】！\n"
        + "══════════════════════════════════════════════════\u001B[0m\n";
    broadcastLog(player, ctx, victoryMsg);

    // 推送戰鬥結束，回到地牢普通狀態
    pushDrpgState(player, pos, null);
  }

  private void resolveDefeat(Player player, BattleContext ctx, DungeonPosition pos) {
    activeBattles.remove(player.getName());

    // 扣除 SAN 並救治至 30% HP
    for (PartyMember m : ctx.getParty().getMembers()) {
      m.consumeSan(20);
      m.setCurrentRage(0);
      m.setCurrentCombo(0);
      if (m.getStats() != null) {
        m.getStats().setHp(Math.max(20, (int)(m.getStats().getMaxHp() * 0.3)));
        m.setAlive(true);
      }
    }

    // 傳送回一層入口 (1, 1)
    if (pos != null) {
      pos.setCoord(1, 1);
    }

    String defeatMsg = "\n\u001B[1;31m═══════════════════【小隊潰敗】═══════════════════\n"
        + "  妖邪陰煞入體，小隊不敵！於千鈞一髮之際激發護身靈符遁回墓塚入口！\n"
        + "  全員身受重傷，道心嚴重受挫 (-20 SAN)！\n"
        + "══════════════════════════════════════════════════\u001B[0m\n";
    broadcastLog(player, ctx, defeatMsg);

    pushDrpgState(player, pos, null);
  }

  private void broadcastLog(Player player, BattleContext ctx, String log) {
    if (ctx != null) {
      ctx.addLog(log);
    }
    if (player != null && player.isValid()) {
      player.reply(log);
    }
  }

  public BattleViewDto createBattleView(String playerName) {
    BattleContext battleCtx = activeBattles.get(playerName);
    if (battleCtx == null || battleCtx.isOver()) {
      return null;
    }
    List<BattleEnemyViewDto> enemyDtos = new ArrayList<>();
    for (int i = 0; i < battleCtx.getEnemies().size(); i++) {
      BattleEnemy e = battleCtx.getEnemies().get(i);
      enemyDtos.add(new BattleEnemyViewDto(
          i,
          e.getId(),
          e.getName(),
          e.getHp(),
          e.getMaxHp(),
          e.getRow().name(),
          e.isAlive(),
          i == battleCtx.getSelectedTargetIndex(),
          e.isStunned()
      ));
    }
    return new BattleViewDto(
        true,
        battleCtx.getState().name(),
        enemyDtos,
        battleCtx.getSelectedTargetIndex(),
        battleCtx.getRecentLogs()
    );
  }

  public void pushDrpgState(Player player, DungeonPosition pos, BattleContext battleCtx) {
    if (player == null) return;
    String floorId = pos != null ? pos.getFloorId() : "taiyin_tomb_b1f";
    DungeonFloor floor = dungeonManager.getFloor(floorId);
    Party party = partyService.getOrCreateParty(player.getName());
    String inspect = (floor != null && pos != null) ? dungeonNavigator.inspectForward(floor, pos) : "";

    BattleViewDto battleView = createBattleView(player.getName());
    DrpgStateDto dto = DrpgStateDto.of(floor, pos, inspect, party, battleView);
    player.sendJson(dto);
  }
}