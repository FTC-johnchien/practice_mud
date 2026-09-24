package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.model.TacticsRule;
import com.example.htmlmud.domain.party.model.TacticsTarget;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 DRPG 戰鬥虛擬執行緒主心跳循環、隊員與怪物行動節奏、走火入魔狀態機與隊友戰術 AI 執行。
 */
@Slf4j
@Service
public class DrpgCombatLoop {

  private final DrpgEnemyTacticsService tacticsService;
  private final DrpgRewardService rewardService;
  private final DefenseResolver defenseResolver;
  private final BuffSettlementService buffSettlementService;

  @Autowired
  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService,
      DefenseResolver defenseResolver, BuffSettlementService buffSettlementService) {
    this.tacticsService = tacticsService != null ? tacticsService : new DrpgEnemyTacticsService();
    this.rewardService = rewardService != null ? rewardService : new DrpgRewardService();
    this.defenseResolver = defenseResolver != null ? defenseResolver : new DefenseResolver();
    this.buffSettlementService = buffSettlementService != null ? buffSettlementService : new BuffSettlementService();
  }

  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService, DefenseResolver defenseResolver) {
    this(tacticsService, rewardService, defenseResolver, new BuffSettlementService());
  }

  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService) {
    this(tacticsService, rewardService, new DefenseResolver(), new BuffSettlementService());
  }

  public DrpgCombatLoop() {
    this(new DrpgEnemyTacticsService(), new DrpgRewardService(), new DefenseResolver(), new BuffSettlementService());
  }

  public BuffSettlementService getBuffSettlementService() {
    return buffSettlementService;
  }

  public DrpgEnemyTacticsService getTacticsService() {
    return tacticsService;
  }

  public DrpgRewardService getRewardService() {
    return rewardService;
  }

  public DefenseResolver getDefenseResolver() {
    return defenseResolver;
  }

  /**
   * 啟動虛擬執行緒戰鬥循環
   */
  public void startBattleLoop(Player player, BattleContext ctx, DungeonPosition pos,
      Map<String, BattleContext> activeBattles, Consumer<Player> stateBroadcaster) {
    String threadName = "BattleLoop-" + (player != null ? player.getName() : "anon");
    Thread.ofVirtual().name(threadName).start(() -> {
      runBattleLoop(player, ctx, pos, activeBattles, stateBroadcaster);
    });
  }

  /**
   * 戰鬥心跳循環核心
   */
  public void runBattleLoop(Player player, BattleContext ctx, DungeonPosition pos,
      Map<String, BattleContext> activeBattles, Consumer<Player> stateBroadcaster) {
    try {
      while (!ctx.isOver() && (player == null || player.isValid())) {
        long now = System.currentTimeMillis();

        // 0. 狀態生命週期結算 (1 Heartbeat = 1 Tick, 結算持續時間、HoT 跳血、DoT 扣血與過期移除)
        for (PartyMember member : ctx.getParty().getMembers()) {
          if (member.isAlive()) {
            List<String> logs = buffSettlementService.processTicks(member);
            logs.forEach(l -> broadcastLog(player, ctx, l));
          }
        }
        for (BattleEnemy enemy : ctx.getEnemies()) {
          if (enemy.isAlive()) {
            List<String> logs = buffSettlementService.processTicks(enemy);
            logs.forEach(l -> broadcastLog(player, ctx, l));
          }
        }
        if (ctx.isAllEnemiesDead()) {
          ctx.setState(BattleState.VICTORY);
          break;
        }
        if (ctx.isAllPartyDead()) {
          ctx.setState(BattleState.DEFEAT);
          break;
        }

        // 1. 我方隊員行動 (包含走火入魔自殘/背刺/異變判定與施法推進)
        for (PartyMember member : ctx.getParty().getMembers()) {
          if (!member.isAlive() || ctx.isOver()) continue;

          // Phase 11: 施法唱條推進 (Casting FSM)
          if (member.isCasting()) {
            String interruptReason = member.popLastInterruptReason();
            if (interruptReason != null) {
              broadcastLog(player, ctx, "\u001B[1;31m💥【施法被打斷】" + member.getName() + " 靈力被震散（" + interruptReason + "），法術中途潰散！\u001B[0m");
            } else if (now >= member.getCastEndTime()) {
              PartyMemberSkill castingSkill = member.getCurrentCastingSkill();
              int targetIdx = member.getCastTargetIdx();
              member.finishCasting();
              broadcastLog(player, ctx, "\u001B[1;36m✨【吟唱完成】" + member.getName() + " 法印締結完畢，真元沛然爆發！\u001B[0m");
              applySkillEffects(player, ctx, member, castingSkill, targetIdx, null, "⚡");
              if (castingSkill.isTriggersGcd()) {
                member.triggerGcd(castingSkill.getGcdMs());
              }
              if (ctx.isOver()) break;
            }
            continue; // 施法中不執行普通攻擊
          } else {
            String interruptReason = member.popLastInterruptReason();
            if (interruptReason != null) {
              broadcastLog(player, ctx, "\u001B[1;31m💥【施法被打斷】" + member.getName() + " 靈力被震散（" + interruptReason + "），法術中途潰散！\u001B[0m");
            }
          }

          if (now >= member.getNextAttackTime()) {
            member.setNextAttackTime(now + member.getAttackIntervalMs());

            // 走火入魔檢定 (非主角隊員)
            boolean isLeader = (player != null && member.getName().equals(player.getName()));
            if (!isLeader) {
              // 若 SAN <= 0 且原本正常，踏入走火入魔第一階段 (CHAOS)
              if (member.getCurrentSan() <= 0 && member.getMadnessState() == PartyMember.MadnessState.SANE) {
                member.setMadnessState(PartyMember.MadnessState.CHAOS);
                member.setAberrationCounter(0);
                broadcastLog(player, ctx, "\u001B[1;35m⚠️【走火入魔】" + member.getName() + " 的道心徹底歸零！古神囈語侵入靈府，雙目泛起血光，進入走火入魔混亂期！異變計數器開始攀升！\u001B[0m");
              }

              // 若處於 SEALED 封印鎮魔狀態，無法出招，暫停計數
              if (member.getMadnessState() == PartyMember.MadnessState.SEALED) {
                continue;
              }

              // 若處於 CHAOS 走火入魔混亂期：每回合累加異變計數器
              if (member.getMadnessState() == PartyMember.MadnessState.CHAOS) {
                int addProgress = ThreadLocalRandom.current().nextInt(15, 30);
                member.setAberrationCounter(member.getAberrationCounter() + addProgress);

                broadcastLog(player, ctx, "\u001B[1;35m👁️【古神低語・異變攀升】" + member.getName() + " 神識渙散，皮膚下肉瘤蠕動，異變進度達 " + member.getAberrationCounter() + "%！\u001B[0m");

                // (3) 異變進度達 100% -> 徹底異化為深淵首領
                if (member.getAberrationCounter() >= 100) {
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
                    int baseDmg = tacticsService.calculatePlayerDamage(member, target);
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

            // 隊友戰術 AI 檢定 (非主角隊員自動依據職責施展急救/嘲諷/絕技)
            if (!isLeader && tryTriggerCompanionTactics(player, ctx, member, pos)) {
              if (ctx.isOver()) break;
              continue;
            }

            // 正常攻擊流程
            BattleEnemy target = (member.getRow() == RowPosition.FRONT)
                ? ctx.getFrontTargetEnemy() : ctx.getTargetEnemy();

            if (target != null && target.isAlive()) {
              int dmg = tacticsService.calculatePlayerDamage(member, target);
              target.takeDamage(dmg);
              member.addThreat(dmg);
              target.addThreat(member.getId(), dmg);

              // 戰氣 SP 動態累積 (普通攻擊命中 +15 SP)
              member.gainSp(15);

              // 相容舊資源計數
              if (member.getResourceType() == CombatResourceType.COMBO) {
                member.gainCombo(1);
              }
              if (member.getResourceType() == CombatResourceType.RAGE) {
                member.gainRage(15);
              }
              // 陣法靈威積累
              ctx.getParty().addFormationEnergy(2);

              // 招式與武器動詞動態呈現
              String moveName = "運勁平擊";
              var move = member.getRandomBasicMove();
              if (move != null && move.name() != null) {
                moveName = move.name();
              }
              var wt = member.getMainHandWeaponType();
              String icon = switch (wt) {
                case SWORD -> "🗡️";
                case BLADE -> "⚔️";
                case BLUNT, HAMMER, MACE, MAUL, CLUB, FLAIL -> "🔨";
                case DAGGER, DIRK, KNIFE, STILETTO -> "⚡";
                case STAFF, WAND, ROD, SCEPTER -> "✨";
                case BOW, CROSSBOW -> "🏹";
                case AXE, POLEAXE -> "🪓";
                case POLEARM, HALBERD, SPEAR, JAVELIN -> "🔱";
                default -> "👊";
              };

              broadcastLog(player, ctx, icon + " " + member.getName() + " 施展【" + moveName + "】，擊中【" + target.getName() + "】造成 " + dmg + " 點傷害！");

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

            PartyMember targetMember = tacticsService.selectPartyTarget(ctx, enemy);
            if (targetMember != null && targetMember.isAlive()) {
              int rawDmg = tacticsService.calculateEnemyDamage(enemy, targetMember);
              String moveName = tacticsService.selectEnemyMove(enemy);

              // 透過 DefenseResolver 執行被動心法檢定 (DODGE / PARRY / FORCE)
              var defenseRes = defenseResolver.resolveEnemyAttack(enemy, targetMember, rawDmg, moveName);
              targetMember.takeDamage(defenseRes.finalDamage());

              String intReason = targetMember.popLastInterruptReason();
              if (intReason != null) {
                broadcastLog(player, ctx, "\u001B[1;31m💥【施法被打斷】" + targetMember.getName() + " 遭受猛烈打擊（" + intReason + "），法術被迫中斷！\u001B[0m");
              }

              if (defenseRes.spGained() > 0) {
                targetMember.gainSp(defenseRes.spGained());
              }
              if (defenseRes.rageGained() > 0) {
                targetMember.gainRage(defenseRes.rageGained());
              }

              if (enemy.getId().startsWith("aberration-")) {
                broadcastLog(player, ctx, "\u001B[1;35m🐙【" + enemy.getName() + "】深淵血肉肉瘤劇烈痙攣，爆發不可名狀凝視，重創 " + targetMember.getName() + " 造成 " + defenseRes.finalDamage() + " 點暗蝕傷害！\u001B[0m");
              } else {
                broadcastLog(player, ctx, defenseRes.combatLog());
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
        if (stateBroadcaster != null && player != null) {
          stateBroadcaster.accept(player);
        }

        Thread.sleep(500);
      }

      // 戰鬥結束結算
      if (ctx.getState() == BattleState.VICTORY) {
        rewardService.resolveVictory(player, ctx, pos, activeBattles, stateBroadcaster);
      } else if (ctx.getState() == BattleState.DEFEAT) {
        rewardService.resolveDefeat(player, ctx, pos, activeBattles, stateBroadcaster);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Battle loop error for player {}", (player != null ? player.getName() : "anon"), e);
    } finally {
      if (player != null && activeBattles != null) {
        if (player.getName() != null) activeBattles.remove(player.getName());
        if (player.getId() != null) activeBattles.remove(player.getId());
      }
      if (stateBroadcaster != null && player != null) {
        stateBroadcaster.accept(player);
      }
    }
  }

  /**
   * 隊友戰術方針觸發評估 (Gambit System)
   */
  public boolean tryTriggerCompanionTactics(Player player, BattleContext ctx, PartyMember member, DungeonPosition pos) {
    if (member == null || !member.isAlive() || member.getSkills() == null || member.getSkills().isEmpty()) {
      return false;
    }

    // 若隊員處於全域冷卻 (GCD) 或正在引導施法中，暫不觸發戰術方針
    if (member.isOnGcd() || member.isCasting()) {
      return false;
    }

    if (member.getTactics() == null || member.getTactics().isEmpty()) {
      member.initDefaultTactics();
    }

    for (TacticsRule rule : member.getTactics()) {
      if (!rule.isEnabled() || rule.getSkillId() == null) continue;

      PartyMemberSkill skill = member.getSkills().stream()
          .filter(s -> s.getId().equalsIgnoreCase(rule.getSkillId()))
          .findFirst()
          .orElse(null);

      if (skill == null || !tacticsService.canCastSkill(member, skill)) {
        continue;
      }

      boolean conditionMet = tacticsService.evaluateTacticsCondition(ctx, member, rule);
      if (!conditionMet) {
        continue;
      }

      // WoW 戰術方針 AI 防呆：若招式附帶 Buff/護盾，且目標隊員身上已存在同 ID 且未過期之狀態，略過該規則防止無效重複連放
      if (skill.getBuffConfig() != null || skill.isShield() || skill.isBuff()) {
        PartyMember targetAlly = tacticsService.resolveAllyTarget(ctx, member,
            (rule.getTarget() != null ? rule.getTarget() : (skill.isShield() ? TacticsTarget.FRONT_ROW_ALLY : TacticsTarget.SELF)), -1);
        String targetBuffId = (skill.getBuffConfig() != null && skill.getBuffConfig().id() != null)
            ? skill.getBuffConfig().id()
            : ("buff_" + skill.getId());
        if (targetAlly != null && targetAlly.hasActiveBuff(targetBuffId)) {
          // 目標已有同名未過期 Buff，跳過此規則執行下一優先級
          continue;
        }
      }

      tacticsService.consumeSkillResource(member, skill);

      // 若為長吟唱法術，進入施法狀態機
      if (skill.getCastTimeMs() > 0) {
        member.startCasting(skill, -1, skill.getCastTimeMs());
        broadcastLog(player, ctx, "\u001B[1;36m🌀【戰術方針】" + member.getName() + " 開始凝氣運轉【" + skill.getName() + "】... (需 "
            + String.format("%.1f", skill.getCastTimeMs() / 1000.0) + " 秒)\u001B[0m");
        if (skill.isTriggersGcd()) {
          member.triggerGcd(skill.getGcdMs());
        }
        return true;
      }

      applySkillEffects(player, ctx, member, skill, -1, rule.getTarget(), "【戰術方針】");
      if (skill.isTriggersGcd()) {
        member.triggerGcd(skill.getGcdMs());
      }
      return true;
    }

    return false;
  }

  /**
   * 執行主動技能效果 (支援單體/AOE、治療、護盾、Buff、嘲諷、暈眩及傷害加成)
   */
  public void applySkillEffects(Player player, BattleContext ctx, PartyMember member,
      PartyMemberSkill skill, int targetIdx, String prefixTag) {
    applySkillEffects(player, ctx, member, skill, targetIdx, null, prefixTag);
  }

  public void applySkillEffects(Player player, BattleContext ctx, PartyMember member,
      PartyMemberSkill skill, int targetIdx, TacticsTarget tacticsTarget, String prefixTag) {
    member.setCooldown(skill.getId(), skill.getCooldownMs());
    ctx.getParty().addFormationEnergy(8);

    String tag = (prefixTag != null && !prefixTag.isEmpty()) ? prefixTag + " " : "";

    if (skill.isHeal()) {
      if (skill.isAoe()) {
        for (PartyMember m : ctx.getParty().getMembers()) {
          if (m.isAlive()) {
            m.heal(skill.getHealAmount());
            if (skill.getSanRestore() > 0) m.restoreSan(skill.getSanRestore());
          }
        }
        member.addThreat(skill.getHealAmount());
        distributeHealingThreatToAllEnemies(ctx, member.getId(), skill.getHealAmount());
        broadcastLog(player, ctx, "\u001B[1;32m" + tag + "✨ " + member.getName() + " 施展【" + skill.getName() + "】，甘露靈泉籠罩全隊！氣血恢復，道心安穩！\u001B[0m");
      } else {
        PartyMember targetAlly = tacticsService.resolveAllyTarget(ctx, member,
            (tacticsTarget != null ? tacticsTarget : TacticsTarget.LOWEST_HP_ALLY), targetIdx);
        targetAlly.heal(skill.getHealAmount());
        if (skill.getSanRestore() > 0) targetAlly.restoreSan(skill.getSanRestore());
        member.addThreat(skill.getHealAmount() / 2);
        distributeHealingThreatToAllEnemies(ctx, member.getId(), skill.getHealAmount() / 2);
        broadcastLog(player, ctx, "\u001B[1;32m" + tag + "🌿 " + member.getName() + " 運轉【" + skill.getName() + "】，一道春生靈氣注入 " + targetAlly.getName() + "，恢復 " + skill.getHealAmount() + " 點氣血！\u001B[0m");
      }
    } else if (skill.isShield()) {
      // 護盾防護技能 (如 金光辟邪護體)
      PartyMember targetAlly = tacticsService.resolveAllyTarget(ctx, member,
          (tacticsTarget != null ? tacticsTarget : TacticsTarget.FRONT_ROW_ALLY), targetIdx);

      ActiveBuff activeBuff = null;
      if (skill.getBuffConfig() != null) {
        activeBuff = buffSettlementService.createActiveBuffFromConfig(skill.getBuffConfig(), targetAlly, member.getId(), skill.getId());
      } else {
        // 資料驅動 fallback
        int targetMaxHp = (targetAlly.getStats() != null) ? targetAlly.getStats().getMaxHp() : 100;
        int baseAmount = (skill.getHealAmount() > 0) ? skill.getHealAmount() : 35;
        int shieldAmount = Math.max((int) (targetMaxHp * 0.25), baseAmount);
        activeBuff = ActiveBuff.builder()
            .id("buff_" + skill.getId())
            .name(skill.getName())
            .icon(skill.getIcon() != null ? skill.getIcon() : "🛡️")
            .type(BuffType.BUFF)
            .category(BuffCategory.SHIELD)
            .durationTicks(40) // 20s
            .remainingTicks(40)
            .value(shieldAmount)
            .build();
      }

      buffSettlementService.applyBuff(targetAlly, activeBuff);
      member.addThreat(activeBuff.getValue() / 2);
      distributeHealingThreatToAllEnemies(ctx, member.getId(), activeBuff.getValue() / 3);
      broadcastLog(player, ctx, "\u001B[1;36m" + tag + "🛡️ " + member.getName() + " 施展【" + skill.getName() + "】，為 "
          + targetAlly.getName() + " 加持辟邪護盾，凝聚 " + activeBuff.getValue() + " 點玄罡金光！（持續 " + activeBuff.getRemainingSeconds() + " 秒）\u001B[0m");

    } else if (skill.isTaunt()) {
      ctx.setTaunt(member.getId(), 5000);
      member.addThreat(600);
      for (BattleEnemy e : ctx.getEnemies()) {
        if (e.isAlive()) {
          e.setTaunt(member.getId(), 5000);
        }
      }
      broadcastLog(player, ctx, "\u001B[1;33m" + tag + "🛡️ " + member.getName() + " 爆發【" + skill.getName() + "】，金剛威儀震懾全場！所有怪物仇恨被強行吸引！\u001B[0m");
    } else if (skill.isBuff() || skill.isDefense()) {
      // 自身減傷或防禦 Buff (如 不動明王)
      PartyMember targetAlly = (tacticsTarget == TacticsTarget.SELF || tacticsTarget == null)
          ? member
          : tacticsService.resolveAllyTarget(ctx, member, tacticsTarget, targetIdx);

      ActiveBuff activeBuff = null;
      if (skill.getBuffConfig() != null) {
        activeBuff = buffSettlementService.createActiveBuffFromConfig(skill.getBuffConfig(), targetAlly, member.getId(), skill.getId());
      } else {
        int maxHp = (targetAlly.getStats() != null) ? targetAlly.getStats().getMaxHp() : 100;
        int barrier = Math.max(30, (int) (maxHp * 0.30));
        activeBuff = ActiveBuff.builder()
            .id("buff_" + skill.getId())
            .name(skill.getName())
            .icon(skill.getIcon() != null ? skill.getIcon() : "⚡")
            .type(BuffType.BUFF)
            .category(BuffCategory.SHIELD)
            .durationTicks(20) // 10s
            .remainingTicks(20)
            .value(barrier)
            .build();
      }

      buffSettlementService.applyBuff(targetAlly, activeBuff);
      member.addThreat(activeBuff.getValue() / 2);
      distributeHealingThreatToAllEnemies(ctx, member.getId(), activeBuff.getValue() / 3);
      broadcastLog(player, ctx, "\u001B[1;33m" + tag + "⚡ " + member.getName() + " 施展【" + skill.getName() + "】，運起不動明王暗金罡氣，周身金芒流轉，生成 "
          + activeBuff.getValue() + " 點不滅金身護體！（持續 " + activeBuff.getRemainingSeconds() + " 秒）\u001B[0m");
    } else {
      // 傷害技能
      if (skill.isAoe()) {
        broadcastLog(player, ctx, "\u001B[1;36m" + tag + "🌩️ " + member.getName() + " 祭出【" + skill.getName() + "】，排山倒海的威能橫掃敵方全體！\u001B[0m");
        for (BattleEnemy e : ctx.getEnemies()) {
          if (e.isAlive()) {
            int dmg = (int) (tacticsService.calculatePlayerDamage(member, e) * skill.getDamageMultiplier());
            e.takeDamage(dmg);
            member.addThreat(dmg);
            e.addThreat(member.getId(), dmg + skill.getThreatBonus());
            broadcastLog(player, ctx, "\u001B[1;36m   ↳ 擊中【" + e.getName() + "】造成 " + dmg + " 點傷害！\u001B[0m");
            if (skill.isStun()) e.applyStun(skill.getStunDurationSeconds() * 1000L);
            if (!e.isAlive()) broadcastLog(player, ctx, "\u001B[1;32m💥【" + e.getName() + "】在靈力轟擊下灰飛煙滅！\u001B[0m");
          }
        }
        if (ctx.isAllEnemiesDead()) ctx.setState(BattleState.VICTORY);
      } else {
        BattleEnemy target = null;
        if (targetIdx >= 0 && targetIdx < ctx.getEnemies().size() && ctx.getEnemies().get(targetIdx).isAlive()) {
          target = ctx.getEnemies().get(targetIdx);
        } else {
          target = (member.getRow() == RowPosition.FRONT) ? ctx.getFrontTargetEnemy() : ctx.getTargetEnemy();
        }
        if (target != null && target.isAlive()) {
          int dmg = (int) (tacticsService.calculatePlayerDamage(member, target) * skill.getDamageMultiplier());
          target.takeDamage(dmg);
          member.addThreat(dmg);
          target.addThreat(member.getId(), dmg + skill.getThreatBonus());
          if (skill.isStun()) target.applyStun(skill.getStunDurationSeconds() * 1000L);
          broadcastLog(player, ctx, "\u001B[1;33m" + tag + "🔥 " + member.getName() + " 施展【" + skill.getName() + "】，直取【" + target.getName() + "】要害，造成 " + dmg + " 點毀滅打擊！\u001B[0m");
          if (!target.isAlive()) {
            broadcastLog(player, ctx, "\u001B[1;32m💥【" + target.getName() + "】慘叫倒地氣絕！\u001B[0m");
            if (ctx.isAllEnemiesDead()) ctx.setState(BattleState.VICTORY);
          }
        }
      }
    }
  }

  private void distributeHealingThreatToAllEnemies(BattleContext ctx, String memberId, int amount) {
    if (ctx == null || ctx.getEnemies() == null || memberId == null || amount <= 0) return;
    long livingCount = ctx.getEnemies().stream().filter(BattleEnemy::isAlive).count();
    if (livingCount <= 0) return;
    int threatPerEnemy = Math.max(1, (int) (amount / livingCount));
    for (BattleEnemy e : ctx.getEnemies()) {
      if (e.isAlive()) {
        e.addThreat(memberId, threatPerEnemy);
      }
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

  private void broadcastLog(Player player, BattleContext ctx, String log) {
    if (ctx != null) {
      ctx.addLog(log);
    }
    if (player != null && player.isValid()) {
      player.reply(log);
    }
  }
}
