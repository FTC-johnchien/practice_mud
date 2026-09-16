package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.enums.ItemType;
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
import com.example.htmlmud.domain.party.model.PartyItemSlot;
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

  public void startTrainingBattle(Player player, DungeonPosition pos) {
    if (isInBattle(player.getName())) {
      player.reply("【試道場】當前已在戰鬥之中！");
      return;
    }
    Party party = partyService.getOrCreateParty(player.getName());

    List<BattleEnemy> enemies = new ArrayList<>();
    // 前排玄鐵試道傀儡（攻極低 1~2，血厚 4000，防禦適中 6）
    BattleEnemy dummyFront = BattleEnemy.builder()
        .id("mob-dummy-1")
        .templateId("taiyin_tomb:training_dummy")
        .name("玄鐵試道傀儡・乾")
        .hp(4000)
        .maxHp(4000)
        .minDamage(1)
        .maxDamage(2)
        .defense(6)
        .dex(8)
        .row(RowPosition.FRONT)
        .attackIntervalMs(2500)
        .nextAttackTime(System.currentTimeMillis() + 1500)
        .alive(true)
        .xp(100)
        .dropItemId("ancient_relic")
        .build();
    enemies.add(dummyFront);

    // 後排避塵試法陣樁（血厚 3000，方便測試後排/AOE群攻技能）
    BattleEnemy dummyBack = BattleEnemy.builder()
        .id("mob-dummy-2")
        .templateId("taiyin_tomb:training_dummy_back")
        .name("避塵試法陣樁・坤")
        .hp(3000)
        .maxHp(3000)
        .minDamage(1)
        .maxDamage(1)
        .defense(4)
        .dex(5)
        .row(RowPosition.BACK)
        .attackIntervalMs(3000)
        .nextAttackTime(System.currentTimeMillis() + 2000)
        .alive(true)
        .xp(100)
        .dropItemId("purify_talisman")
        .build();
    enemies.add(dummyBack);

    BattleContext ctx = BattleContext.builder()
        .battleId("training-" + player.getName() + "-" + System.currentTimeMillis())
        .playerId(player.getName())
        .party(party)
        .enemies(enemies)
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    long now = System.currentTimeMillis();
    for (PartyMember m : party.getMembers()) {
      m.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(600, 1600));
    }

    broadcastLog(player, ctx, "\n\u001B[1;36m🎯【試道演武場開啟】太陰玄鐵試道傀儡已就位（血厚攻低）！請盡情測試隊員絕學與陣法奧義！\u001B[0m");
    activeBattles.put(player.getName(), ctx);
    pushDrpgState(player, pos, ctx);

    Thread.ofVirtual().name("TrainingBattleLoop-" + player.getName()).start(() -> {
      runBattleLoop(player, ctx, pos);
    });
  }

  public void chargeCombatResources(Player player, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    // 陣法靈威補滿 100
    party.setFormationEnergy(100);
    // 各隊員戰意與真元補滿
    for (PartyMember m : party.getMembers()) {
      if (m.isAlive()) {
        m.setCurrentRage(m.getMaxRage());
        m.setCurrentCombo(m.getMaxCombo());
        if (m.getStats() != null) {
          m.getStats().setMp(m.getStats().getMaxMp());
        }
      }
    }
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx != null) {
      broadcastLog(player, ctx, "\u001B[1;33m⚡【靈威充能完畢】陣法靈威達 100 點（大招就緒）！全員怒氣/連擊點/真元全滿！\u001B[0m");
      pushDrpgState(player, pos, ctx);
    } else {
      player.reply("⚡【充能完畢】陣法靈威已充至 100 點！全員戰鬥能量充盈！");
      pushDrpgState(player, pos, null);
    }
  }

  public void startBattle(Player player, DungeonPosition pos, List<String> mobTemplateIds) {
    Party party = partyService.getOrCreateParty(player.getName());

    List<BattleEnemy> enemies = new ArrayList<>();
    int idx = 0;
    for (String templateId : mobTemplateIds) {
      idx++;
      var opt = TemplateRepository.findMob(templateId);
      String name = opt.map(MobTemplate::name).orElse("太陰妖邪") + " " + (char)('A' + idx - 1);
      int maxHp = 220 + (idx * 60);
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

        // 1. 我方隊員行動 (包含走火入魔自殘/背刺/異變判定)
        for (PartyMember member : ctx.getParty().getMembers()) {
          if (!member.isAlive() || ctx.isOver()) continue;

          if (now >= member.getNextAttackTime()) {
            member.setNextAttackTime(now + member.getAttackIntervalMs());

            // 走火入魔檢定 (非主角隊員)
            boolean isLeader = member.getName().equals(player.getName());
            if (!isLeader) {
              // 若 SAN <= 0 且原本正常，踏入走火入魔第一階段 (CHAOS)
              if (member.getCurrentSan() <= 0 && member.getMadnessState() == PartyMember.MadnessState.SANE) {
                member.setMadnessState(PartyMember.MadnessState.CHAOS);
                member.setAberrationCounter(0);
                broadcastLog(player, ctx, "\u001B[1;35m⚠️【走火入魔】" + member.getName() + " 的道心徹底歸零！古神囈語侵入靈府，雙目泛起血光，進入走火入魔混亂期！異變計數器開始攀升！\u001B[0m");
              }

              // 若處於 SEALED 封印鎮魔狀態，無法出招，暫停計數
              if (member.getMadnessState() == PartyMember.MadnessState.SEALED) {
                broadcastLog(player, ctx, "\u001B[1;34m🔒【鎮魔封印中】" + member.getName() + " 身貼鎖神符，異變暫停，無法行動！\u001B[0m");
                continue;
              }

              // 若處於第一階段 CHAOS
              if (member.getMadnessState() == PartyMember.MadnessState.CHAOS) {
                // (1) 血肉反噬：每回合自殘 15% 最大 HP
                int recoil = Math.max(1, (int) (member.getStats().getMaxHp() * 0.15));
                member.takeDamage(recoil);
                broadcastLog(player, ctx, "\u001B[1;31m🩸【血肉反噬】" + member.getName() + " 體內煞氣逆衝，自殘嘔血扣除 " + recoil + " 點氣血 (當前 HP: " + member.getStats().getHp() + ")！\u001B[0m");

                // 若自殘至 HP 歸 0，未竟異變而亡，化為爛肉死肉 (Permadeath)
                if (!member.isAlive()) {
                  member.setMadnessState(PartyMember.MadnessState.DEAD_MEAT);
                  broadcastLog(player, ctx, "\u001B[1;31m💀【血肉崩解】" + member.getName() + " 尚未完成異變，肉身承受不住古神煞氣，直接爆裂為一灘死肉黑血！神魂俱滅！\u001B[0m");
                  continue;
                }

                // (2) 異變失控計數器累加 25
                int counter = member.getAberrationCounter() + 25;
                member.setAberrationCounter(counter);
                broadcastLog(player, ctx, "\u001B[1;35m👁️【異變加深】" + member.getName() + " 體表生出無數蠕動觸手肉瘤！異變失控進度：" + counter + "%！\u001B[0m");

                // (3) 滿 100 進入第二階段 (ABERRATION)
                if (counter >= 100) {
                  member.setMadnessState(PartyMember.MadnessState.ABERRATION);
                  member.setAlive(false); // 移出我方活人隊伍

                  int bossHp = Math.max(200, member.getStats().getMaxHp() * 10);
                  PartyMemberSkill firstSkill = !member.getSkills().isEmpty() ? member.getSkills().get(0) : null;
                  String sId = firstSkill != null ? firstSkill.getId() : "sword_pierce";
                  String sName = firstSkill != null ? firstSkill.getName() : "道種遺學";

                  BattleEnemy boss = BattleEnemy.builder()
                      .id("aberration-" + member.getId())
                      .templateId("taiyin:aberration_boss")
                      .name("【深淵畸變體・" + member.getName() + "】")
                      .hp(bossHp)
                      .maxHp(bossHp)
                      .minDamage(member.getEffectiveMinDamage() * 2)
                      .maxDamage(member.getEffectiveMaxDamage() * 2 + 10)
                      .defense(member.getEffectiveDefense() + 6)
                      .dex(12)
                      .row(RowPosition.FRONT)
                      .attackIntervalMs(2000)
                      .nextAttackTime(now + 1000)
                      .alive(true)
                      .xp(250)
                      .dropItemId("core_" + member.getId() + "_" + sId)
                      .droppedDaoSkillId(sId)
                      .droppedDaoSkillName(sName)
                      .droppedDaoMemberName(member.getName())
                      .build();

                  ctx.getEnemies().add(boss);
                  broadcastLog(player, ctx, "\n\u001B[1;35m⚡⚡【不可名狀畸變・降世】深淵裂隙撕開！" + member.getName() + " 的肉身徹底畸變為域外古神眷族【深淵畸變體・" + member.getName() + "】！氣血暴增 10 倍 (" + bossHp + " HP)！狂暴敵對！\u001B[0m\n");
                  continue;
                }

                // (4) 混亂出手：傷害暴增 100% (x2.0)，40% 背刺隊友
                if (ThreadLocalRandom.current().nextInt(100) < 40) {
                  PartyMember victim = selectRandomTeammateExcept(ctx.getParty(), member);
                  if (victim != null && victim.isAlive()) {
                    int baseDmg = ThreadLocalRandom.current().nextInt(member.getEffectiveMinDamage(), member.getEffectiveMaxDamage() + 1);
                    int rawDmg = (int) (Math.max(1, baseDmg - victim.getEffectiveDefense()) * 2.0);
                    victim.takeDamage(rawDmg);
                    broadcastLog(player, ctx, "\u001B[1;31m🩸【心魔背刺】" + member.getName() + " 神智癲狂，竟將身旁的 " + victim.getName() + " 視為妖邪，魔焰暴增 100% 狠下殺手，造成 " + rawDmg + " 點重創！\u001B[0m");
                    if (!victim.isAlive()) {
                      broadcastLog(player, ctx, "\u001B[1;31m💀 " + victim.getName() + " 慘遭失控隊友斬殺倒地！\u001B[0m");
                    }
                    continue;
                  }
                } else {
                  // 60% 狂轟敵方
                  BattleEnemy target = (member.getRow() == RowPosition.FRONT) ? ctx.getFrontTargetEnemy() : ctx.getTargetEnemy();
                  if (target != null && target.isAlive()) {
                    int baseDmg = calculatePlayerDamage(member, target);
                    int boostedDmg = baseDmg * 2;
                    target.takeDamage(boostedDmg);
                    broadcastLog(player, ctx, "\u001B[1;35m🔥【魔道凶威】" + member.getName() + " 狂亂暴怒，爆發走火入魔之煞氣，對【" + target.getName() + "】造成 " + boostedDmg + " 點雙倍狂暴打擊！\u001B[0m");
                    if (!target.isAlive()) {
                      broadcastLog(player, ctx, "\u001B[1;32m💥【" + target.getName() + "】被斬殺倒地！\u001B[0m");
                      if (ctx.isAllEnemiesDead()) {
                        ctx.setState(BattleState.VICTORY);
                        break;
                      }
                    }
                    continue;
                  }
                }
              }
            }

            // 正常攻擊流程
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

        // 2. 敵方怪物自動攻擊 (包含深淵畸變體)
        for (BattleEnemy enemy : ctx.getEnemies()) {
          if (!enemy.isAlive() || enemy.isStunned() || ctx.isOver()) continue;

          if (now >= enemy.getNextAttackTime()) {
            enemy.setNextAttackTime(now + enemy.getAttackIntervalMs());

            PartyMember targetMember = selectPartyTarget(ctx);
            if (targetMember != null && targetMember.isAlive()) {
              int rawDmg = ThreadLocalRandom.current().nextInt(enemy.getMinDamage(), enemy.getMaxDamage() + 1);
              int finalDmg = Math.max(1, rawDmg - targetMember.getEffectiveDefense());
              targetMember.takeDamage(finalDmg);

              // 力士受傷累積怒氣 (+15)
              if (targetMember.getResourceType() == ResourceType.RAGE) {
                targetMember.gainRage(15);
              }

              if (enemy.getId().startsWith("aberration-")) {
                broadcastLog(player, ctx, "\u001B[1;35m🐙【" + enemy.getName() + "】深淵血肉肉瘤劇烈痙攣，爆發不可名狀凝視，重創 " + targetMember.getName() + " 造成 " + finalDmg + " 點暗蝕傷害！\u001B[0m");
              } else {
                broadcastLog(player, ctx, "\u001B[1;31m⚡【" + enemy.getName() + "】爪擊撕咬，重創 " + targetMember.getName() + " 造成 " + finalDmg + " 點傷害！\u001B[0m");
              }

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

  private PartyMember selectRandomTeammateExcept(Party party, PartyMember self) {
    if (party == null || party.getMembers() == null) return null;
    List<PartyMember> candidates = party.getMembers().stream()
        .filter(m -> m != self && m.isAlive())
        .toList();
    if (candidates.isEmpty()) return null;
    return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
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
      postBattleCleanup(player, ctx);
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

    // 重置戰鬥資源
    for (PartyMember m : ctx.getParty().getMembers()) {
      m.setCurrentRage(0);
      m.setCurrentCombo(0);
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

    // 2. 普通戰利品掉落入背包
    String dropItemId = TOMB_ITEMS.get(ThreadLocalRandom.current().nextInt(TOMB_ITEMS.size()));
    var opt = TemplateRepository.findItem(dropItemId);
    String dropName = opt.map(ItemTemplate::name).orElse("太陰靈物");
    boolean added = ctx.getParty().getInventory().addItem(dropItemId, 1);
    if (added) {
      droppedItemNames.add(dropName);
    }

    // 3. 戰後清理 (解開封印 / 移除永久死肉與異變者)
    postBattleCleanup(player, ctx);

    String lootSummary = droppedItemNames.isEmpty() ? "（行囊已滿，未能裝入物品）" : String.join("、", droppedItemNames);

    String victoryMsg = "\n\u001B[1;32m═══════════════════【戰鬥大捷】═══════════════════\n"
        + "  小隊合力斬除太陰妖邪！全員獲得修為 " + totalXp + " 點！\n"
        + "  戰利品已收納入隊伍行囊：【" + lootSummary + "】！\n"
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

    postBattleCleanup(player, ctx);

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

  private void postBattleCleanup(Player player, BattleContext ctx) {
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

  /**
   * 施展太上鎮魔封印術
   */
  public void sealTarget(Player player, int targetIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    // 1. 戰鬥中可封印敵方古神畸變體
    if (ctx != null && !ctx.isOver() && targetIdx >= 0 && targetIdx < ctx.getEnemies().size()) {
      BattleEnemy e = ctx.getEnemies().get(targetIdx);
      if (e.isAlive() && e.getId().startsWith("aberration-")) {
        e.applyStun(99999999L);
        broadcastLog(player, ctx, "\u001B[1;34m🔒【太上鎮魔大篆】天師金符凌空鎮壓！【" + e.getName() + "】全身畸變肉瘤被死死封禁，失去所有反抗與行動能力！\u001B[0m");
        pushDrpgState(player, pos, ctx);
        return;
      }
    }

    // 2. 封印走火入魔的我方隊友
    if (targetIdx >= 0 && targetIdx < party.getMembers().size()) {
      PartyMember m = party.getMembers().get(targetIdx);
      if (m.getName().equals(player.getName())) {
        player.reply("【鎮魔】身為隊長，道心受浩然天罡守護，無需對自己施展封印術！");
        return;
      }
      if (m.getMadnessState() == PartyMember.MadnessState.CHAOS) {
        m.setMadnessState(PartyMember.MadnessState.SEALED);
        broadcastLog(player, ctx, "\u001B[1;34m🔒【太上鎮魔符】施展封印秘術！" + m.getName() + " 眉心貼上鎖神靈符，異變計數器凍結在 " + m.getAberrationCounter() + "%！暫時失去所有行動能力，直到戰鬥結束方可解封！\u001B[0m");
        pushDrpgState(player, pos, ctx);
        return;
      } else if (m.getMadnessState() == PartyMember.MadnessState.SEALED) {
        player.reply(m.getName() + " 當前已處於封印鎮魔狀態！");
        return;
      } else {
        player.reply(m.getName() + " 並未走火入魔，無須施展鎮魔封印。");
        return;
      }
    }

    player.reply("無效的封印目標編號！可封印走火入魔的隊友 (0~5)，或戰場上的深淵畸變體！");
  }

  /**
   * 使用行囊物品
   */
  public void useItem(Player player, String slotIdOrIndex, int memberIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    String realSlotId = resolveSlotId(party, slotIdOrIndex);
    if (realSlotId == null) {
      player.reply("行囊中找不到該物品！");
      return;
    }

    String result = party.useItemOnMember(realSlotId, memberIdx);
    broadcastLog(player, ctx, result);
    pushDrpgState(player, pos, ctx);
  }

  /**
   * 裝備法寶防具
   */
  public void equipItem(Player player, String slotIdOrIndex, int memberIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    String realSlotId = resolveSlotId(party, slotIdOrIndex);
    if (realSlotId == null) {
      player.reply("行囊中找不到該物品！");
      return;
    }

    String result = party.equipItemOnMember(realSlotId, memberIdx);
    broadcastLog(player, ctx, result);
    pushDrpgState(player, pos, ctx);
  }

  /**
   * 卸下裝備
   */
  public void unequipItem(Player player, String slotType, int memberIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    String result = party.unequipItemFromMember(slotType, memberIdx);
    broadcastLog(player, ctx, result);
    pushDrpgState(player, pos, ctx);
  }

  private String resolveSlotId(Party party, String slotIdOrIndex) {
    if (slotIdOrIndex == null || party.getInventory() == null) return null;
    if (party.getInventory().getItem(slotIdOrIndex) != null) {
      return slotIdOrIndex;
    }
    try {
      int idx = Integer.parseInt(slotIdOrIndex);
      if (idx >= 1 && idx <= party.getInventory().getSlots().size()) {
        return party.getInventory().getSlots().get(idx - 1).getSlotId();
      } else if (idx >= 0 && idx < party.getInventory().getSlots().size()) {
        return party.getInventory().getSlots().get(idx).getSlotId();
      }
    } catch (NumberFormatException ignored) {}

    for (PartyItemSlot s : party.getInventory().getSlots()) {
      if (s.getItemId().equalsIgnoreCase(slotIdOrIndex) || s.getName().contains(slotIdOrIndex)) {
        return s.getSlotId();
      }
    }
    return null;
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