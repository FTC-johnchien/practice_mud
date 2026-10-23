package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.CombatResourceType;
import com.example.htmlmud.party.RowPosition;
import com.example.htmlmud.party.TacticsRule;
import com.example.htmlmud.party.TacticsTarget;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.model.enums.WeaponType;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEvent;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEventType;
import com.example.htmlmud.domain.dungeon.battle.event.BattleHit;
import com.example.htmlmud.domain.dungeon.battle.event.FxShape;
import com.example.htmlmud.domain.dungeon.battle.event.HitOutcome;
import com.example.htmlmud.domain.dungeon.battle.event.UnitRef;
import com.example.htmlmud.template.TemplateCatalog;
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
  private final com.example.htmlmud.party.FormationEngine formationEngine;
  private final CombatFsmService combatFsmService;
  private final ExecutorService combatExecutor;
  @Autowired(required = false)
  private ComboResolver comboResolver = new ComboResolver();


  @Autowired
  public DrpgCombatLoop(
      DrpgEnemyTacticsService tacticsService,
      DrpgRewardService rewardService,
      DefenseResolver defenseResolver,
      BuffSettlementService buffSettlementService,
      com.example.htmlmud.party.FormationEngine formationEngine,
      CombatFsmService combatFsmService,
      @Qualifier("combatExecutor") @Autowired(required = false) ExecutorService combatExecutor) {
    this.tacticsService = tacticsService != null ? tacticsService : new DrpgEnemyTacticsService();
    this.rewardService = rewardService != null ? rewardService : new DrpgRewardService();
    this.defenseResolver = defenseResolver != null ? defenseResolver : new DefenseResolver();
    this.buffSettlementService = buffSettlementService != null ? buffSettlementService : new BuffSettlementService();
    this.formationEngine = formationEngine != null ? formationEngine : new com.example.htmlmud.party.FormationEngine();
    this.combatFsmService = combatFsmService != null ? combatFsmService : new CombatFsmService();
    this.combatExecutor = combatExecutor != null ? combatExecutor
        : Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("drpg-combat-fallback-", 0).factory());
  }

  public DrpgCombatLoop(
      DrpgEnemyTacticsService tacticsService,
      DrpgRewardService rewardService,
      DefenseResolver defenseResolver,
      BuffSettlementService buffSettlementService,
      com.example.htmlmud.party.FormationEngine formationEngine,
      @Qualifier("combatExecutor") @Autowired(required = false) ExecutorService combatExecutor) {
    this(tacticsService, rewardService, defenseResolver, buffSettlementService, formationEngine, new CombatFsmService(), combatExecutor);
  }

  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService,
      DefenseResolver defenseResolver, BuffSettlementService buffSettlementService,
      com.example.htmlmud.party.FormationEngine formationEngine) {
    this(tacticsService, rewardService, defenseResolver, buffSettlementService, formationEngine, null);
  }

  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService,
      DefenseResolver defenseResolver, BuffSettlementService buffSettlementService) {
    this(tacticsService, rewardService, defenseResolver, buffSettlementService, new com.example.htmlmud.party.FormationEngine(), null);
  }

  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService, DefenseResolver defenseResolver) {
    this(tacticsService, rewardService, defenseResolver, new BuffSettlementService(), new com.example.htmlmud.party.FormationEngine(), null);
  }

  public DrpgCombatLoop(DrpgEnemyTacticsService tacticsService, DrpgRewardService rewardService) {
    this(tacticsService, rewardService, new DefenseResolver(), new BuffSettlementService(), new com.example.htmlmud.party.FormationEngine(), null);
  }

  public DrpgCombatLoop() {
    this(new DrpgEnemyTacticsService(), new DrpgRewardService(), new DefenseResolver(), new BuffSettlementService(), new com.example.htmlmud.party.FormationEngine(), null);
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

  public CombatFsmService getCombatFsmService() {
    return combatFsmService;
  }

  public ExecutorService getCombatExecutor() {
    return combatExecutor;
  }

  /**
   * 啟動受治理的虛擬執行緒戰鬥循環
   */
  public Future<?> startBattleLoop(Player player, BattleContext ctx, DungeonPosition pos,
      Map<String, BattleContext> activeBattles, Consumer<Player> stateBroadcaster) {
    Future<?> future = combatExecutor.submit(() -> {
      runBattleLoop(player, ctx, pos, activeBattles, stateBroadcaster);
    });
    if (ctx != null) {
      ctx.setCombatFuture(future);
    }
    return future;
  }

  /**
   * 戰鬥心跳循環核心
   */
  public void runBattleLoop(Player player, BattleContext ctx, DungeonPosition pos,
      Map<String, BattleContext> activeBattles, Consumer<Player> stateBroadcaster) {
    try {
      while (!ctx.isOver() && (player == null || player.isValid())) {
        if (Thread.currentThread().isInterrupted()) {
          log.info("Battle loop thread interrupted for player {}", (player != null ? player.getName() : "anon"));
          break;
        }

        ctx.setRoundCount(ctx.getRoundCount() + 1);

        // 逾時或放置超時熔斷斷路器 (ACT-02 / Combat-02)
        if (ctx.isTimedOut() || ctx.isMaxRoundsExceeded()) {
          ctx.setState(BattleState.TIMEOUT);
          String reason = ctx.isIdleTimedOut()
              ? "小隊長久無人指揮 (超過 " + (ctx.getMaxIdleDurationMs() / 60000) + " 分鐘無操作)"
              : (ctx.isMaxRoundsExceeded()
                  ? "戰鬥超過極限回合上限 (" + ctx.getMaxRounds() + " 回合)"
                  : "戰鬥超時 (超過 " + (ctx.getMaxDurationMs() / 1000) + " 秒)");
          log.warn("戰鬥熔斷斷路器觸發: {} for player {}", reason, (player != null ? player.getName() : "anon"));
          broadcastLog(player, ctx, "\n\u001B[1;31m⚡【心神渙散】" + reason + "，天地靈氣劇烈震盪，陣法自動潰散脫離戰鬥！\u001B[0m\n");
          break;
        }

        long now = System.currentTimeMillis();
        ctx.setLastHeartbeat(now);

        // 0. 狀態生命週期結算 (1 Heartbeat = 1 Tick, 結算持續時間、HoT 跳血、DoT 扣血與過期移除)
        for (PartyMember member : ctx.getParty().getMembers()) {
          if (member.isAlive()) {
            List<String> logs = buffSettlementService.processTicks(member);
            logs.forEach(l -> broadcastLog(player, ctx, l));
            if (!member.isAlive()) {
              handlePartyMemberDeath(player, ctx, member);
            }
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
            member.setNextAttackTime(now + member.getEffectiveAttackIntervalMs());

            // 精神萎靡檢定 (非主角隊員)
            boolean isLeader = (player != null && member.getName().equals(player.getName()));
            if (!isLeader) {
              // 若 SAN <= 0 且原本正常，進入精神萎靡狀態 (CHAOS)
              if (member.getCurrentSan() <= 0 && member.getMadnessState() == PartyMember.MadnessState.SANE) {
                member.setMadnessState(PartyMember.MadnessState.CHAOS);
                broadcastLog(player, ctx, "\u001B[1;33m⚠️【精神萎靡】" + member.getName() + " 的理智徹底耗盡！陷入精神萎靡狀態（攻擊威力降低 15%）！\u001B[0m");
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
              boolean isStaggerCrit = target.isPoiseBroken();
              int dmg = tacticsService.calculatePlayerDamage(member, target);
              // 若處於精神萎靡狀態，威力衰減 15%
              if (member.getMadnessState() == PartyMember.MadnessState.CHAOS) {
                dmg = Math.max(1, (int) (dmg * 0.85));
              }
              if (isStaggerCrit) {
                dmg = com.example.htmlmud.domain.combat.CombatRules.applyPoiseBreakCrit(dmg, true);
              }
              target.takeDamage(dmg);
              checkAndTriggerPhaseTransition(player, ctx, target);
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

              if (isStaggerCrit) {
                broadcastLog(player, ctx, "\u001B[1;31m💥【破防重創!】" + icon + " " + member.getName() + " 施展【" + moveName + "】直取要害，對力竭破防之【" + target.getName() + "】造成 " + dmg + " 點致命暴擊傷害！\u001B[0m");
              } else {
                broadcastLog(player, ctx, icon + " " + member.getName() + " 施展【" + moveName + "】，擊中【" + target.getName() + "】造成 " + dmg + " 點傷害！");
              }

              int targetIdx = ctx.getEnemies().indexOf(target);
              int memberIdx = ctx.getParty().getMembers().indexOf(member);
              boolean isKilled = !target.isAlive();
              com.example.htmlmud.domain.dungeon.battle.event.HitOutcome hitOutcome = isStaggerCrit
                  ? com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.CRIT
                  : com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.HIT;
              com.example.htmlmud.domain.dungeon.battle.event.BattleEvent atkEvent = new com.example.htmlmud.domain.dungeon.battle.event.BattleEvent(
                  ctx.nextEventSeq(),
                  now,
                  com.example.htmlmud.domain.dungeon.battle.event.BattleEventType.ATTACK,
                  new com.example.htmlmud.domain.dungeon.battle.event.UnitRef(com.example.htmlmud.domain.dungeon.battle.event.UnitRef.Side.PARTY, member.getId(), memberIdx),
                  null,
                  moveName,
                  wt.name(),
                  resolveWeaponDamageType(wt),
                  com.example.htmlmud.domain.dungeon.battle.event.FxShape.SINGLE,
                  "weapon_" + wt.name().toLowerCase(),
                  java.util.List.of(new com.example.htmlmud.domain.dungeon.battle.event.BattleHit(
                      new com.example.htmlmud.domain.dungeon.battle.event.UnitRef(com.example.htmlmud.domain.dungeon.battle.event.UnitRef.Side.ENEMY, target.getId(), targetIdx >= 0 ? targetIdx : 0),
                      hitOutcome,
                      dmg,
                      0,
                      isKilled
                  ))
              );
              ctx.emit(atkEvent);

              if (!target.isAlive()) {
                broadcastLog(player, ctx, "\u001B[1;32m💥【" + target.getName() + "】被斬殺倒地！\u001B[0m");
                ctx.checkEnemyRowAdvancement();
                if (ctx.isAllEnemiesDead()) {
                  ctx.setState(BattleState.VICTORY);
                  break;
                }
              }
            }
          }
        }

        if (ctx.isOver()) break;

        // 2. 敵方怪物自動攻擊 (包含深淵畸變體) 與行為預告 (ENEMY-INTENT-01)
        for (BattleEnemy enemy : ctx.getEnemies()) {
          if (!enemy.isAlive() || ctx.isOver()) continue;

          // 眩暈/失衡狀態檢查：若正在蓄力則打斷
          if (enemy.isStunned()) {
            if (enemy.isCasting()) {
              enemy.clearIntent();
              broadcastLog(player, ctx, "\u001B[1;33m💫 " + enemy.getName() + " 陷入眩暈，蓄力打擊被迫中斷！\u001B[0m");
            }
            continue;
          }

          // 初始化初始攻擊倒數
          if (enemy.getNextAttackTime() == 0) {
            enemy.setNextAttackTime(now + Math.min(enemy.getAttackIntervalMs(), ThreadLocalRandom.current().nextLong(800, 1600)));
          }

          // 施法與蓄力推進 (Combat Engine FSM)
          String enemyInterrupt = enemy.popLastInterruptReason();
          if (enemyInterrupt != null) {
            broadcastLog(player, ctx, "\u001B[1;31m💥【怪物施法被打斷】" + enemy.getName() + " 蓄力潰散（" + enemyInterrupt + "）！\u001B[0m");
            continue;
          }

          // Telegraph 行為預告階段：在攻擊前預告意圖、鎖定目標、啟動蓄力條 (CASTING)
          long timeUntilAttack = enemy.getNextAttackTime() - now;
          long triggerWindow = enemy.getCastDurationMs() > 0 ? enemy.getCastDurationMs() : 1500L;
          if (enemy.getCurrentIntentName() == null && timeUntilAttack <= triggerWindow) {
            PartyMember prospectiveTarget = tacticsService.selectPartyTarget(ctx, enemy);
            if (prospectiveTarget != null && prospectiveTarget.isAlive()) {
              String prospectiveMove = tacticsService.selectEnemyMove(enemy);
              String icon = "🗡️";
              String type = "PHYSICAL";
              String moveDisplayName = prospectiveMove != null ? prospectiveMove : "揮砍攻擊";

              if (enemy.getId() != null && enemy.getId().startsWith("aberration-")) {
                icon = "👁️";
                type = "ABERRATION";
                moveDisplayName = "不可名狀凝視";
              } else if (enemy.isLarge() || (enemy.getRank() != null && enemy.getRank() != MobRank.NORMAL)) {
                icon = "⚔️";
                type = "HEAVY";
                if (prospectiveMove == null) moveDisplayName = "狂暴猛擊";
              } else if (prospectiveMove != null && (prospectiveMove.contains("咒") || prospectiveMove.contains("法") || prospectiveMove.contains("雷") || prospectiveMove.contains("火"))) {
                icon = prospectiveMove.contains("雷") ? "⚡" : "🔥";
                type = "SPELL";
              } else if (prospectiveMove != null && (prospectiveMove.contains("撕咬") || prospectiveMove.contains("爪") || prospectiveMove.contains("毒"))) {
                icon = "🐾";
                type = "PHYSICAL";
              }

              long castDuration = enemy.getCastDurationMs() > 0 ? enemy.getCastDurationMs() : Math.max(500, timeUntilAttack);
              boolean interruptible = enemy.isInterruptible();
              combatFsmService.startEnemyCast(enemy, icon, moveDisplayName, type, "SINGLE", castDuration, interruptible);
            }
          }

          if (now >= enemy.getNextAttackTime()) {
            String preparedMove = enemy.getCurrentIntentName();
            enemy.finishCast();
            enemy.setNextAttackTime(now + enemy.getEffectiveAttackIntervalMs());

            PartyMember targetMember = tacticsService.selectPartyTarget(ctx, enemy);
            if (targetMember != null && targetMember.isAlive()) {
              int rawDmg = tacticsService.calculateEnemyDamage(enemy, targetMember);
              String moveName = preparedMove != null ? preparedMove : tacticsService.selectEnemyMove(enemy);

              // 透過 DefenseResolver 執行被動心法檢定 (DODGE / PARRY / FORCE)
              var defenseRes = defenseResolver.resolveEnemyAttack(enemy, targetMember, rawDmg, moveName);
              targetMember.takeDamage(defenseRes.finalDamage());

              // 破招反擊 (Riposte) 傷害結算
              if (defenseRes.riposteTriggered() && defenseRes.riposteDamage() > 0) {
                enemy.takeDamage(defenseRes.riposteDamage());
                checkAndTriggerPhaseTransition(player, ctx, enemy);
                if (!enemy.isAlive()) {
                  broadcastLog(player, ctx, "\u001B[1;31m💀 " + enemy.getName() + " 被 " + targetMember.getName() + " 的破招突刺擊殺倒地！\u001B[0m");
                }
              }

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

              // 敵方攻擊事件發佈 (帶入 DefenseResolver 判定結果)
              int enemyIdx = ctx.getEnemies().indexOf(enemy);
              int mIdx = ctx.getParty().getMembers().indexOf(targetMember);
              com.example.htmlmud.domain.dungeon.battle.event.HitOutcome hitOutcome = switch (defenseRes.outcome()) {
                case CRIT -> com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.CRIT;
                case BLOCKED -> com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.BLOCKED;
                case PARRIED -> com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.PARRIED;
                case DODGED -> com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.DODGED;
                case MISS -> com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.MISS;
                default -> com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.HIT;
              };

              com.example.htmlmud.domain.dungeon.battle.event.BattleEvent enemyAtkEvent = new com.example.htmlmud.domain.dungeon.battle.event.BattleEvent(
                  ctx.nextEventSeq(),
                  now,
                  com.example.htmlmud.domain.dungeon.battle.event.BattleEventType.ATTACK,
                  new com.example.htmlmud.domain.dungeon.battle.event.UnitRef(com.example.htmlmud.domain.dungeon.battle.event.UnitRef.Side.ENEMY, enemy.getId(), enemyIdx >= 0 ? enemyIdx : 0),
                  null,
                  moveName,
                  "NATURAL",
                  enemy.getId().startsWith("aberration-") ? "DARK" : "PHYSICAL",
                  com.example.htmlmud.domain.dungeon.battle.event.FxShape.SINGLE,
                  null,
                  java.util.List.of(new com.example.htmlmud.domain.dungeon.battle.event.BattleHit(
                      new com.example.htmlmud.domain.dungeon.battle.event.UnitRef(com.example.htmlmud.domain.dungeon.battle.event.UnitRef.Side.PARTY, targetMember.getId(), mIdx >= 0 ? mIdx : 0),
                      hitOutcome,
                      defenseRes.finalDamage(),
                      0,
                      !targetMember.isAlive()
                  ))
              );
              ctx.emit(enemyAtkEvent);

              // 破招突刺反擊事件
              if (defenseRes.riposteTriggered() && defenseRes.riposteDamage() > 0) {
                com.example.htmlmud.domain.dungeon.battle.event.BattleEvent riposteEvent = new com.example.htmlmud.domain.dungeon.battle.event.BattleEvent(
                    ctx.nextEventSeq(),
                    now,
                    com.example.htmlmud.domain.dungeon.battle.event.BattleEventType.RIPOSTE,
                    new com.example.htmlmud.domain.dungeon.battle.event.UnitRef(com.example.htmlmud.domain.dungeon.battle.event.UnitRef.Side.PARTY, targetMember.getId(), mIdx >= 0 ? mIdx : 0),
                    null,
                    "破招反擊",
                    "SWORD",
                    "PHYSICAL",
                    com.example.htmlmud.domain.dungeon.battle.event.FxShape.SINGLE,
                    null,
                    java.util.List.of(new com.example.htmlmud.domain.dungeon.battle.event.BattleHit(
                        new com.example.htmlmud.domain.dungeon.battle.event.UnitRef(com.example.htmlmud.domain.dungeon.battle.event.UnitRef.Side.ENEMY, enemy.getId(), enemyIdx >= 0 ? enemyIdx : 0),
                        com.example.htmlmud.domain.dungeon.battle.event.HitOutcome.HIT,
                        defenseRes.riposteDamage(),
                        0,
                        !enemy.isAlive()
                    ))
                );
                ctx.emit(riposteEvent);
              }

              if (!targetMember.isAlive()) {
                handlePartyMemberDeath(player, ctx, targetMember);
                if (ctx.isAllPartyDead()) {
                  ctx.setState(BattleState.DEFEAT);
                  break;
                }
              }
            }
          }
        }

        // 推送戰鬥演出事件 (先送事件、後送快照)
        java.util.List<com.example.htmlmud.domain.dungeon.battle.event.BattleEvent> events = ctx.drainEvents();
        if (!events.isEmpty() && player != null && player.isValid()) {
          player.sendJson(new com.example.htmlmud.domain.dungeon.dto.BattleEventsDto(ctx.getBattleId(), events));
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
      } else if (ctx.getState() == BattleState.TIMEOUT) {
        rewardService.postBattleCleanup(player, ctx);
        if (player != null) {
          player.reply("戰鬥因時限已至或靈氣枯竭而終止。");
        }
      }
      if (ctx.getOnBattleEnd() != null) {
        try {
          ctx.getOnBattleEnd().accept(ctx.getState());
        } catch (Exception ex) {
          log.error("執行 onBattleEnd 回呼異常 for player {}", (player != null ? player.getName() : "anon"), ex);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.info("Battle loop thread interrupted for player {}", (player != null ? player.getName() : "anon"));
    } catch (Exception e) {
      log.error("Battle loop error for player {}", (player != null ? player.getName() : "anon"), e);
    } finally {
      if (ctx != null && player != null && player.isValid()) {
        java.util.List<com.example.htmlmud.domain.dungeon.battle.event.BattleEvent> finalEvents = ctx.drainEvents();
        if (!finalEvents.isEmpty()) {
          player.sendJson(new com.example.htmlmud.domain.dungeon.dto.BattleEventsDto(ctx.getBattleId(), finalEvents));
        }
      }
      if (activeBattles != null) {
        if (player != null) {
          if (player.getName() != null) activeBattles.remove(player.getName());
          if (player.getId() != null) activeBattles.remove(player.getId());
        }
        if (ctx != null) {
          if (ctx.getPlayerId() != null) activeBattles.remove(ctx.getPlayerId());
          if (ctx.getBattleId() != null) activeBattles.remove(ctx.getBattleId());
        }
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

    // 若隊員已停用戰術方針 (Gambit Disabled)，不自動觸發戰術
    if (!member.isGambitEnabled()) {
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

      // WoW 戰術方針 AI 防呆：若招式附帶 Buff/護盾，且目標隊員身上已存在同 ID 且剩餘時效 > 3 秒 (或常駐)，略過該規則防止無效重複連放
      if (skill.getBuffConfig() != null || skill.isShield() || skill.isBuff()) {
        PartyMember targetAlly = tacticsService.resolveAllyTarget(ctx, member, rule, -1);
        String targetBuffId = (skill.getBuffConfig() != null && skill.getBuffConfig().id() != null)
            ? skill.getBuffConfig().id()
            : ("buff_" + skill.getId());
        if (targetAlly != null) {
          ActiveBuff existing = targetAlly.getActiveBuff(targetBuffId);
          if (existing != null && (existing.isPermanent() || existing.getRemainingSeconds() > 3)) {
            // 目標已有同名 Buff 且時效 > 3 秒，跳過此規則執行下一優先級
            continue;
          }
        }
      }

      tacticsService.consumeSkillResource(member, skill);

      // 若為長吟唱法術，進入施法狀態機
      if (skill.getCastTimeMs() > 0) {
        int castingTargetIdx = -1;
        if (rule.getTarget() != null) {
          BattleEnemy et = tacticsService.resolveEnemyTarget(ctx, rule.getTarget());
          if (et != null) {
            castingTargetIdx = ctx.getEnemies().indexOf(et);
          }
        }
        member.startCasting(skill, castingTargetIdx, skill.getCastTimeMs());
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

  public static String resolveWeaponDamageType(WeaponType wt) {
    if (wt == null) return "PHYSICAL";
    return switch (wt) {
      case SWORD, BLADE, AXE, POLEAXE, KATANA, SABER, SCIMITAR -> "SLASH";
      case SPEAR, POLEARM, HALBERD, DAGGER, DIRK, KNIFE, STILETTO, BOW, CROSSBOW, JAVELIN, DART, SHURIKEN -> "PIERCE";
      case BLUNT, HAMMER, MACE, MAUL, CLUB, FLAIL, STONE -> "BLUNT";
      case STAFF, WAND, ROD, SCEPTER -> "MAGIC";
      default -> "PHYSICAL";
    };
  }

  public static String resolveSkillDamageType(PartyMemberSkill skill, PartyMember member) {
    if (skill == null) return "PHYSICAL";
    if (skill.getDamageType() != null && !skill.getDamageType().isBlank() && !"PHYSICAL".equalsIgnoreCase(skill.getDamageType())) {
      return skill.getDamageType().toUpperCase();
    }
    if (skill.getTags() != null) {
      for (String tag : skill.getTags()) {
        String upper = tag.toUpperCase();
        if (upper.equals("FIRE") || upper.equals("ICE") || upper.equals("LIGHTNING")
            || upper.equals("POISON") || upper.equals("HOLY") || upper.equals("DARK")
            || upper.equals("MAGIC") || upper.equals("SONIC") || upper.equals("SLASH")
            || upper.equals("PIERCE") || upper.equals("BLUNT")) {
          return upper;
        }
      }
    }
    String combined = ((skill.getId() != null ? skill.getId() : "") + " " + (skill.getName() != null ? skill.getName() : "")).toLowerCase();
    if (combined.contains("fire") || combined.contains("炎") || combined.contains("火")) return "FIRE";
    if (combined.contains("ice") || combined.contains("frost") || combined.contains("冰") || combined.contains("寒")) return "ICE";
    if (combined.contains("thunder") || combined.contains("lightning") || combined.contains("雷") || combined.contains("電")) return "LIGHTNING";
    if (combined.contains("holy") || combined.contains("聖") || combined.contains("光") || combined.contains("甘露")) return "HOLY";
    if (combined.contains("dark") || combined.contains("shadow") || combined.contains("暗") || combined.contains("煞") || combined.contains("魔")) return "DARK";
    if (combined.contains("poison") || combined.contains("毒")) return "POISON";

    if (member != null && member.getMainHandWeaponType() != null) {
      return resolveWeaponDamageType(member.getMainHandWeaponType());
    }
    return "PHYSICAL";
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
    long now = System.currentTimeMillis();
    int memberIdx = ctx.getParty().getMembers().indexOf(member);
    String weaponType = member.getMainHandWeaponType() != null ? member.getMainHandWeaponType().name() : "UNARMED";
    String skillDmgType = resolveSkillDamageType(skill, member);

    if (skill.isHeal()) {
      boolean isCleanse = (skill.getTags() != null && skill.getTags().contains("CLEANSE"));

      if (skill.isAoe()) {
        List<BattleHit> hits = new java.util.ArrayList<>();
        for (PartyMember m : ctx.getParty().getMembers()) {
          if (m.isAlive()) {
            m.heal(skill.getHealAmount());
            if (skill.getSanRestore() > 0) m.restoreSan(skill.getSanRestore());
            if (isCleanse) {
              m.getActiveBuffs().removeIf(b -> b.getCategory() == com.example.htmlmud.domain.model.enums.BuffCategory.DOT
                  || b.getCategory() == com.example.htmlmud.domain.model.enums.BuffCategory.CONTROL
                  || b.getType() == com.example.htmlmud.domain.model.enums.BuffType.DEBUFF);
            }
            int mIdx = ctx.getParty().getMembers().indexOf(m);
            hits.add(new BattleHit(
                new UnitRef(UnitRef.Side.PARTY, m.getId(), mIdx >= 0 ? mIdx : 0),
                HitOutcome.HIT,
                skill.getHealAmount(),
                0,
                false
            ));
          }
        }
        member.addThreat(skill.getHealAmount());
        distributeHealingThreatToAllEnemies(ctx, member.getId(), skill.getHealAmount());
        broadcastLog(player, ctx, "\u001B[1;32m" + tag + "✨ " + member.getName() + " 施展【" + skill.getName() + "】，甘露靈泉籠罩全隊！氣血恢復，道心安穩！\u001B[0m");
        BattleEvent healEvent = new BattleEvent(
            ctx.nextEventSeq(),
            now,
            BattleEventType.HEAL,
            new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
            skill.getId(),
            skill.getName(),
            weaponType,
            "HOLY",
            FxShape.ALLY_ALL,
            skill.getFxKey() != null ? skill.getFxKey() : "heal_aoe",
            hits
        );
        ctx.emit(healEvent);
      } else {
        PartyMember targetAlly = tacticsService.resolveAllyTarget(ctx, member,
            (tacticsTarget != null ? tacticsTarget : TacticsTarget.LOWEST_HP_ALLY), targetIdx);
        targetAlly.heal(skill.getHealAmount());
        if (skill.getSanRestore() > 0) targetAlly.restoreSan(skill.getSanRestore());
        if (isCleanse) {
          int beforeCount = targetAlly.getActiveBuffs().size();
          targetAlly.getActiveBuffs().removeIf(b -> b.getCategory() == com.example.htmlmud.domain.model.enums.BuffCategory.DOT
              || b.getCategory() == com.example.htmlmud.domain.model.enums.BuffCategory.CONTROL
              || b.getType() == com.example.htmlmud.domain.model.enums.BuffType.DEBUFF);
          if (targetAlly.getActiveBuffs().size() < beforeCount) {
            broadcastLog(player, ctx, "\u001B[1;32m✨ " + targetAlly.getName() + " 身中之劇毒與陰煞邪氣被【" + skill.getName() + "】滌蕩一空，重獲清明！\u001B[0m");
          }
        }
        member.addThreat(skill.getHealAmount() / 2);
        distributeHealingThreatToAllEnemies(ctx, member.getId(), skill.getHealAmount() / 2);
        broadcastLog(player, ctx, "\u001B[1;32m" + tag + "🌿 " + member.getName() + " 運轉【" + skill.getName() + "】，一道春生靈氣注入 " + targetAlly.getName() + "，恢復 " + skill.getHealAmount() + " 點氣血！\u001B[0m");
        int targetMemberIdx = ctx.getParty().getMembers().indexOf(targetAlly);
        BattleEvent healEvent = new BattleEvent(
            ctx.nextEventSeq(),
            now,
            BattleEventType.HEAL,
            new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
            skill.getId(),
            skill.getName(),
            weaponType,
            "HOLY",
            FxShape.ALLY_SINGLE,
            skill.getFxKey() != null ? skill.getFxKey() : "heal_single",
            List.of(new BattleHit(
                new UnitRef(UnitRef.Side.PARTY, targetAlly.getId(), targetMemberIdx >= 0 ? targetMemberIdx : 0),
                HitOutcome.HIT,
                skill.getHealAmount(),
                0,
                false
            ))
        );
        ctx.emit(healEvent);
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

      int targetMemberIdx = ctx.getParty().getMembers().indexOf(targetAlly);
      BattleEvent shieldEvent = new BattleEvent(
          ctx.nextEventSeq(),
          now,
          BattleEventType.SHIELD,
          new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
          skill.getId(),
          skill.getName(),
          weaponType,
          "HOLY",
          FxShape.ALLY_SINGLE,
          skill.getFxKey() != null ? skill.getFxKey() : "shield",
          List.of(new BattleHit(
              new UnitRef(UnitRef.Side.PARTY, targetAlly.getId(), targetMemberIdx >= 0 ? targetMemberIdx : 0),
              HitOutcome.HIT,
              activeBuff.getValue(),
              0,
              false
          ))
      );
      ctx.emit(shieldEvent);

    } else if (skill.isTaunt()) {
      ctx.setTaunt(member.getId(), 5000);
      member.addThreat(600);
      List<BattleHit> hits = new java.util.ArrayList<>();
      for (BattleEnemy e : ctx.getEnemies()) {
        if (e.isAlive()) {
          e.setTaunt(member.getId(), 5000);
          int eIdx = ctx.getEnemies().indexOf(e);
          hits.add(new BattleHit(
              new UnitRef(UnitRef.Side.ENEMY, e.getId(), eIdx >= 0 ? eIdx : 0),
              HitOutcome.HIT,
              0,
              0,
              false
          ));
        }
      }
      broadcastLog(player, ctx, "\u001B[1;33m" + tag + "🛡️ " + member.getName() + " 爆發【" + skill.getName() + "】，金剛威儀震懾全場！所有怪物仇恨被強行吸引！\u001B[0m");
      BattleEvent tauntEvent = new BattleEvent(
          ctx.nextEventSeq(),
          now,
          BattleEventType.SKILL,
          new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
          skill.getId(),
          skill.getName(),
          weaponType,
          "TAUNT",
          FxShape.ALL,
          "taunt_roar",
          hits
      );
      ctx.emit(tauntEvent);
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

      int targetMemberIdx = ctx.getParty().getMembers().indexOf(targetAlly);
      BattleEvent buffEvent = new BattleEvent(
          ctx.nextEventSeq(),
          now,
          BattleEventType.BUFF,
          new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
          skill.getId(),
          skill.getName(),
          weaponType,
          "HOLY",
          (targetAlly == member) ? FxShape.SELF : FxShape.ALLY_SINGLE,
          skill.getFxKey() != null ? skill.getFxKey() : "buff",
          List.of(new BattleHit(
              new UnitRef(UnitRef.Side.PARTY, targetAlly.getId(), targetMemberIdx >= 0 ? targetMemberIdx : 0),
              HitOutcome.HIT,
              activeBuff.getValue(),
              0,
              false
          ))
      );
      ctx.emit(buffEvent);
    } else {
      // 傷害技能
      DamageType dt = DamageType.PHYSICAL;
      try {
        dt = DamageType.valueOf(skillDmgType.toUpperCase());
      } catch (Exception ignored) {}

      if (skill.isAoe()) {
        broadcastLog(player, ctx, "\u001B[1;36m" + tag + "🌩️ " + member.getName() + " 祭出【" + skill.getName() + "】，排山倒海的威能橫掃敵方全體！\u001B[0m");
        List<BattleHit> hits = new java.util.ArrayList<>();
        for (BattleEnemy e : ctx.getEnemies()) {
          if (e.isAlive()) {
            boolean isStaggerCrit = e.isPoiseBroken();
            int rawDmg = (int) (tacticsService.calculatePlayerDamage(member, e) * skill.getDamageMultiplier());
            if (isStaggerCrit) {
              rawDmg = com.example.htmlmud.domain.combat.CombatRules.applyPoiseBreakCrit(rawDmg, true);
            }
            var resResult = com.example.htmlmud.domain.combat.CombatRules.resolveResistance(rawDmg, dt, e.getResistance(dt));
            int dmg = resResult.damage();
            int healAmt = resResult.heal();

            if (healAmt > 0) {
              e.heal(healAmt);
              broadcastLog(player, ctx, "\u001B[1;36m   ↳ ☯️【靈氣逆流】" + e.getName() + " 吸收 " + dt + " 威能，反獲治療 " + healAmt + " 點氣血！\u001B[0m");
            } else {
              int[] dmgRef = new int[]{dmg};
              int[] poiseDmgRef = new int[]{skill.getPoiseDamage()};
              resolveElementalReactionAndDetonation(player, ctx, member, skill, e, dt, dmgRef, poiseDmgRef);
              dmg = dmgRef[0];
              int finalPoiseDmg = poiseDmgRef[0];

              e.takeDamage(dmg);
              checkAndTriggerPhaseTransition(player, ctx, e);
              if (isStaggerCrit) {
                broadcastLog(player, ctx, "\u001B[1;31m   ↳ 💥【破防重創!】擊中力竭之【" + e.getName() + "】造成 " + dmg + " 點致命暴擊傷害！\u001B[0m");
              } else {
                broadcastLog(player, ctx, "\u001B[1;36m   ↳ 擊中【" + e.getName() + "】造成 " + dmg + " 點傷害！\u001B[0m");
              }
              if (finalPoiseDmg > 0) {
                e.applyPoiseDamage(finalPoiseDmg);
              }
            }

            if (skill.getBuffConfig() != null && e.isAlive()) {
              ActiveBuff debuff = buffSettlementService.createActiveBuffFromConfig(skill.getBuffConfig(), e, member.getId(), skill.getId());
              if (debuff != null) {
                buffSettlementService.applyBuff(e, debuff);
                broadcastLog(player, ctx, "\u001B[1;35m   ↳ " + debuff.getIcon() + "【" + e.getName() + "】身中【" + debuff.getName() + "】！（持續 " + debuff.getRemainingSeconds() + " 秒）\u001B[0m");
              }
            }

            // 蓄力打斷判定 (Combat React: 標籤打斷 + 破防打斷)
            var interruptRes = combatFsmService.resolveSkillInterrupt(e, member, skill);
            if (!interruptRes.interrupted() && e.isPoiseBroken()) {
              interruptRes = combatFsmService.resolvePoiseBreakInterrupt(e, member);
            }
            if (interruptRes.interrupted()) {
              broadcastLog(player, ctx, interruptRes.combatLog());
            }

            member.addThreat(dmg);
            e.addThreat(member.getId(), dmg + skill.getThreatBonus());
            if (skill.isStun()) e.applyStun(skill.getStunDurationSeconds() * 1000L);
            boolean isKilled = !e.isAlive();
            int eIdx = ctx.getEnemies().indexOf(e);
            HitOutcome hitOutcome = interruptRes.interrupted()
                ? HitOutcome.INTERRUPTED
                : (isStaggerCrit ? HitOutcome.CRIT : HitOutcome.HIT);
            hits.add(new BattleHit(
                new UnitRef(UnitRef.Side.ENEMY, e.getId(), eIdx >= 0 ? eIdx : 0),
                hitOutcome,
                dmg,
                0,
                isKilled
            ));
            if (isKilled) {
              broadcastLog(player, ctx, "\u001B[1;32m💥【" + e.getName() + "】在靈力轟擊下灰飛煙滅！\u001B[0m");
              ctx.checkEnemyRowAdvancement();
            }
          }
        }
        if (!hits.isEmpty()) {
          BattleEvent aoeEvent = new BattleEvent(
              ctx.nextEventSeq(),
              now,
              BattleEventType.SKILL,
              new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
              skill.getId(),
              skill.getName(),
              weaponType,
              skillDmgType,
              FxShape.ALL,
              skill.getFxKey() != null ? skill.getFxKey() : "aoe_" + skillDmgType.toLowerCase(),
              hits
          );
          ctx.emit(aoeEvent);
        }
        if (ctx.isAllEnemiesDead()) ctx.setState(BattleState.VICTORY);
      } else {
        BattleEnemy target = null;
        if (tacticsTarget != null) {
          target = tacticsService.resolveEnemyTarget(ctx, tacticsTarget);
        }
        if (target == null || !target.isAlive()) {
          if (targetIdx >= 0 && targetIdx < ctx.getEnemies().size() && ctx.getEnemies().get(targetIdx).isAlive()) {
            target = ctx.getEnemies().get(targetIdx);
          } else {
            target = (member.getRow() == RowPosition.FRONT) ? ctx.getFrontTargetEnemy() : ctx.getTargetEnemy();
          }
        }
        if (target != null && target.isAlive()) {
          boolean isStaggerCrit = target.isPoiseBroken();
          int rawDmg = (int) (tacticsService.calculatePlayerDamage(member, target) * skill.getDamageMultiplier());
          if (isStaggerCrit) {
            rawDmg = com.example.htmlmud.domain.combat.CombatRules.applyPoiseBreakCrit(rawDmg, true);
          }
          var resResult = com.example.htmlmud.domain.combat.CombatRules.resolveResistance(rawDmg, dt, target.getResistance(dt));
          int dmg = resResult.damage();
          int healAmt = resResult.heal();

          if (healAmt > 0) {
            target.heal(healAmt);
            broadcastLog(player, ctx, "\u001B[1;36m" + tag + "☯️【靈氣逆流】" + target.getName() + " 運轉道體，將【" + skill.getName() + "】之 " + dt + " 威能吸收，反獲治療 " + healAmt + " 點氣血！\u001B[0m");
          } else {
            int[] dmgRef = new int[]{dmg};
            int[] poiseDmgRef = new int[]{skill.getPoiseDamage()};
            resolveElementalReactionAndDetonation(player, ctx, member, skill, target, dt, dmgRef, poiseDmgRef);
            dmg = dmgRef[0];
            int finalPoiseDmg = poiseDmgRef[0];

            target.takeDamage(dmg);
            checkAndTriggerPhaseTransition(player, ctx, target);
            if (isStaggerCrit) {
              broadcastLog(player, ctx, "\u001B[1;31m" + tag + "💥【破防重創!】🔥 " + member.getName() + " 施展【" + skill.getName() + "】直取力竭要害，對【" + target.getName() + "】造成 " + dmg + " 點致命暴擊傷害！\u001B[0m");
            } else {
              broadcastLog(player, ctx, "\u001B[1;33m" + tag + "🔥 " + member.getName() + " 施展【" + skill.getName() + "】，直取【" + target.getName() + "】要害，造成 " + dmg + " 點打擊！\u001B[0m");
            }
            if (finalPoiseDmg > 0) {
              target.applyPoiseDamage(finalPoiseDmg);
            }
          }

          if (skill.getBuffConfig() != null && target.isAlive()) {
            ActiveBuff debuff = buffSettlementService.createActiveBuffFromConfig(skill.getBuffConfig(), target, member.getId(), skill.getId());
            if (debuff != null) {
              buffSettlementService.applyBuff(target, debuff);
              broadcastLog(player, ctx, "\u001B[1;35m   ↳ " + debuff.getIcon() + "【" + target.getName() + "】身中【" + debuff.getName() + "】！（持續 " + debuff.getRemainingSeconds() + " 秒）\u001B[0m");
            }
          }

          // 蓄力打斷判定 (Combat React: 標籤打斷 + 破防打斷)
          var interruptRes = combatFsmService.resolveSkillInterrupt(target, member, skill);
          if (!interruptRes.interrupted() && target.isPoiseBroken()) {
            interruptRes = combatFsmService.resolvePoiseBreakInterrupt(target, member);
          }
          if (interruptRes.interrupted()) {
            broadcastLog(player, ctx, interruptRes.combatLog());
          }

          member.addThreat(dmg);
          target.addThreat(member.getId(), dmg + skill.getThreatBonus());
          if (skill.isStun()) target.applyStun(skill.getStunDurationSeconds() * 1000L);
          boolean isKilled = !target.isAlive();
          int eIdx = ctx.getEnemies().indexOf(target);
          HitOutcome singleOutcome = interruptRes.interrupted()
              ? HitOutcome.INTERRUPTED
              : (isStaggerCrit ? HitOutcome.CRIT : HitOutcome.HIT);
          BattleEvent singleSkillEvent = new BattleEvent(
              ctx.nextEventSeq(),
              now,
              BattleEventType.SKILL,
              new UnitRef(UnitRef.Side.PARTY, member.getId(), memberIdx >= 0 ? memberIdx : 0),
              skill.getId(),
              skill.getName(),
              weaponType,
              skillDmgType,
              FxShape.SINGLE,
              skill.getFxKey() != null ? skill.getFxKey() : "skill_" + skillDmgType.toLowerCase(),
              List.of(new BattleHit(
                  new UnitRef(UnitRef.Side.ENEMY, target.getId(), eIdx >= 0 ? eIdx : 0),
                  singleOutcome,
                  dmg,
                  0,
                  isKilled
              ))
          );
          ctx.emit(singleSkillEvent);
          if (isKilled) {
            broadcastLog(player, ctx, "\u001B[1;32m💥【" + target.getName() + "】慘叫倒地氣絕！\u001B[0m");
            ctx.checkEnemyRowAdvancement();
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

  private void handlePartyMemberDeath(Player player, BattleContext ctx, PartyMember member) {
    if (member == null || member.isAlive()) return;
    broadcastLog(player, ctx, "\u001B[1;35m💀 " + member.getName() + " 力竭倒下！\u001B[0m");
    if (formationEngine != null && ctx.getParty() != null) {
      String breakMsg = formationEngine.checkAndBreakFormation(ctx.getParty(), member.getName() + " 力竭倒下");
      if (breakMsg != null) {
        broadcastLog(player, ctx, "\u001B[1;31m⚠️ " + breakMsg + "\u001B[0m");
      }
    }
  }

  private void checkAndTriggerPhaseTransition(Player player, BattleContext ctx, BattleEnemy enemy) {
    if (enemy == null || !enemy.isAlive()) return;
    int nextPhase = enemy.checkPhaseTransition();
    if (nextPhase <= enemy.getCurrentPhase()) return;

    enemy.transitionToPhase(nextPhase);
    long now = System.currentTimeMillis();
    int enemyIdx = ctx.getEnemies().indexOf(enemy);

    // 震退打斷全體小隊成員施法與蓄力 (Shockwave Interrupt)
    List<BattleHit> shockwaveHits = new java.util.ArrayList<>();
    if (ctx.getParty() != null && ctx.getParty().getMembers() != null) {
      for (int mIdx = 0; mIdx < ctx.getParty().getMembers().size(); mIdx++) {
        var member = ctx.getParty().getMembers().get(mIdx);
        if (member.isAlive()) {
          member.setNextAttackTime(Math.max(member.getNextAttackTime(), now + 800L));
          shockwaveHits.add(new BattleHit(
              new UnitRef(UnitRef.Side.PARTY, member.getId(), mIdx),
              HitOutcome.INTERRUPTED,
              0,
              0,
              false
          ));
        }
      }
    }

    String phaseTitle = enemy.isEnraged()
        ? "⚡⚡⚡【超載狂暴・第 " + nextPhase + " 階段】"
        : "⚡⚡【轉階段・第 " + nextPhase + " 階段】";

    if (enemy.isEnraged()) {
      broadcastLog(player, ctx, "\u001B[1;31m" + phaseTitle + enemy.getName() + " 體內古代能量核心狂暴過載！氣血跌破閾值，爆發毀滅性靈壓震退全隊！\u001B[0m");
      broadcastLog(player, ctx, "\u001B[1;35m   ↳ 💥 震散全體負面狀態，罡氣屏障重聚暴漲至 " + enemy.getMaxPoise() + " 點，魔道攻速全面激化！\u001B[0m");
    } else {
      broadcastLog(player, ctx, "\u001B[1;33m" + phaseTitle + enemy.getName() + " 受創激怒，體內禁制崩解，爆發氣浪震退全隊！\u001B[0m");
      broadcastLog(player, ctx, "\u001B[1;36m   ↳ 🛡️ 負面狀態全數淨化，罡氣屏障重聚攀升至 " + enemy.getMaxPoise() + " 點！\u001B[0m");
    }

    BattleEvent transitionEvent = new BattleEvent(
        ctx.nextEventSeq(),
        now,
        BattleEventType.SKILL,
        new UnitRef(UnitRef.Side.ENEMY, enemy.getId(), enemyIdx >= 0 ? enemyIdx : 0),
        "phase_transition",
        enemy.isEnraged() ? "超載狂暴" : "氣浪震退",
        "NONE",
        "ENERGY",
        FxShape.ALL,
        enemy.isEnraged() ? "boss_enraged" : "boss_phase_transition",
        shockwaveHits
    );
    ctx.emit(transitionEvent);

    // 激化立即出手或縮減下次出手前搖
    enemy.setNextAttackTime(now + 400L);
  }

  /**
   * 結算元素反應與裂魂印爆 (F2-05)
   */
  private void resolveElementalReactionAndDetonation(
      Player player, BattleContext ctx, PartyMember member, PartyMemberSkill skill,
      BattleEnemy target, DamageType dt, int[] dmgRef, int[] poiseDmgRef
  ) {
    if (target == null || !target.isAlive() || ctx == null) return;

    // 1. 元素反應判定 (火+冰/水 -> 氣化爆裂, 雷+水/冰 -> 感電傳導, 暗+光 -> 混沌湮滅, 火+毒 -> 三昧燎原)
    var reactionResult = com.example.htmlmud.domain.combat.CombatRules.evaluateElementalReaction(
        dt, dmgRef[0], poiseDmgRef[0], target.getActiveBuffs());
    if (reactionResult.hasReaction()) {
      dmgRef[0] = reactionResult.damage();
      poiseDmgRef[0] = reactionResult.poiseDamage();
      if (reactionResult.logMessage() != null) {
        broadcastLog(player, ctx, "   ↳ " + reactionResult.logMessage());
      }
      if (reactionResult.stunDurationMs() > 0) {
        target.applyStun(reactionResult.stunDurationMs());
      }
      if (reactionResult.consumedBuffId() != null) {
        target.getActiveBuffs().removeIf(b -> b.getId().equals(reactionResult.consumedBuffId()));
      }
      // 感電傳導：若 chainToRow 則對同排其他存活敵怪施加傳導打擊
      if (reactionResult.chainToRow()) {
        int chainDmg = Math.max(1, (int) Math.round(dmgRef[0] * 0.5));
        for (BattleEnemy allyEnemy : ctx.getEnemies()) {
          if (allyEnemy != target && allyEnemy.isAlive() && allyEnemy.getRow() == target.getRow()) {
            allyEnemy.takeDamage(chainDmg);
            allyEnemy.applyPoiseDamage(10);
            broadcastLog(player, ctx, "\u001B[1;36m      ⚡【電弧跳躍】雷煞電弧蔓延至同排【"
                + allyEnemy.getName() + "】，造成 " + chainDmg + " 點感電傳導傷害！\u001B[0m");
            checkAndTriggerPhaseTransition(player, ctx, allyEnemy);
            if (!allyEnemy.isAlive()) {
              broadcastLog(player, ctx, "\u001B[1;32m💥【" + allyEnemy.getName() + "】在連鎖電弧中灰飛煙滅！\u001B[0m");
              ctx.checkEnemyRowAdvancement();
            }
          }
        }
      }
    }

    // 2. 裂魂印記 (Primer) 與引爆戰技 (Detonator) 判定
    boolean isDetonator = (skill != null && skill.isDetonator());
    var detonationResult = com.example.htmlmud.domain.combat.CombatRules.evaluateDetonation(
        isDetonator, 0, target.getActiveBuffs());
    if (detonationResult.triggered()) {
      dmgRef[0] += detonationResult.singleTargetDamage();
      poiseDmgRef[0] += detonationResult.bonusPoiseDamage();
      if (detonationResult.logMessage() != null) {
        broadcastLog(player, ctx, "   ↳ " + detonationResult.logMessage());
      }
      if (detonationResult.consumedBuffIds() != null && !detonationResult.consumedBuffIds().isEmpty()) {
        target.getActiveBuffs().removeIf(b -> detonationResult.consumedBuffIds().contains(b.getId()));
      }
      // 全場衝擊波 (AOE Shockwave)
      if (detonationResult.aoeDamage() > 0) {
        int aoeDmg = detonationResult.aoeDamage();
        for (BattleEnemy otherEnemy : ctx.getEnemies()) {
          if (otherEnemy != target && otherEnemy.isAlive()) {
            otherEnemy.takeDamage(aoeDmg);
            otherEnemy.applyPoiseDamage(15);
            broadcastLog(player, ctx, "\u001B[1;31m      🌊【衝擊波波及】全場衝擊波震盪，【"
                + otherEnemy.getName() + "】承受 " + aoeDmg + " 點餘波震盪！\u001B[0m");
            checkAndTriggerPhaseTransition(player, ctx, otherEnemy);
            if (!otherEnemy.isAlive()) {
              broadcastLog(player, ctx, "\u001B[1;32m💥【" + otherEnemy.getName() + "】被震盪衝擊波粉碎！\u001B[0m");
              ctx.checkEnemyRowAdvancement();
            }
          }
        }
      }
    }

    // 3. 古仙陣法跨隊員連攜合擊判定 (F3-01 Cross Combos)
    if (comboResolver != null) {
      var comboRes = comboResolver.processCrossCombo(
          player, ctx, member, target, dmgRef[0], poiseDmgRef[0],
          (p, l) -> broadcastLog(p, ctx, l));
      if (comboRes.chainAdvanced()) {
        dmgRef[0] += comboRes.bonusDamage();
        poiseDmgRef[0] += comboRes.bonusPoiseDamage();
      }
    }
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

