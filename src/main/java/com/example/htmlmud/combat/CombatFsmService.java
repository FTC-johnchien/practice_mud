package com.example.htmlmud.combat;

import java.util.List;
import org.springframework.stereotype.Service;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import lombok.extern.slf4j.Slf4j;

/**
 * 戰鬥狀態機服務 (Combat Finite State Machine Service)
 * 負責怪物施法前搖 (CASTING)、蓄力時間線推移、即時反制打斷判定與失衡破防處理 (STAGGER / POISE_BREAK)。
 */
@Slf4j
@Service
public class CombatFsmService {

  public record InterruptResult(
      boolean interrupted,
      String interruptedAction,
      long stunDurationMs,
      String combatLog
  ) {
    public static InterruptResult notInterrupted() {
      return new InterruptResult(false, null, 0L, null);
    }
  }

  /**
   * 啟動怪物蓄力施法階段 (CASTING)
   */
  public void startEnemyCast(
      BattleEnemy enemy,
      String icon,
      String actionName,
      String type,
      String scope,
      long durationMs,
      boolean interruptible
  ) {
    startEnemyCast(enemy, icon, actionName, type, scope, durationMs, interruptible, null);
  }

  /**
   * 啟動怪物蓄力施法階段 (CASTING) 並關聯具體技能 ID
   */
  public void startEnemyCast(
      BattleEnemy enemy,
      String icon,
      String actionName,
      String type,
      String scope,
      long durationMs,
      boolean interruptible,
      String skillId
  ) {
    if (enemy == null || !enemy.isAlive()) return;
    enemy.startIntent(icon, actionName, type, scope, durationMs, interruptible, skillId);
    log.debug("Monster [{}] entered CASTING state for [{}] (skillId={}, duration={}ms, interruptible={})",
        enemy.getName(), actionName, skillId, durationMs, interruptible);
  }

  /**
   * 檢定技能是否具備打斷反制能力 (包含 STUN、SILENCE、CONTROL、SEAL、PARRY、BREAK、INTERRUPT 標籤或 stun 屬性)
   */
  public boolean canInterrupt(PartyMemberSkill skill) {
    if (skill == null) return false;
    if (skill.isStun()) return true;
    List<String> tags = skill.getTags();
    if (tags != null) {
      for (String tag : tags) {
        if ("STUN".equalsIgnoreCase(tag)
            || "SILENCE".equalsIgnoreCase(tag)
            || "CONTROL".equalsIgnoreCase(tag)
            || "SEAL".equalsIgnoreCase(tag)
            || "PARRY".equalsIgnoreCase(tag)
            || "BREAK".equalsIgnoreCase(tag)
            || "INTERRUPT".equalsIgnoreCase(tag)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 當怪物受到招式打擊時，執行即時反制打斷判定：
   * 若怪物處於可打斷的施法蓄力中 (isCasting && isInterruptible)，且招式帶有打斷標籤，
   * 則立即重置蓄力、清除意圖，並使怪物進入破防失衡虛弱狀態 (Poise Broken & Staggered)。
   */
  public InterruptResult resolveSkillInterrupt(BattleEnemy enemy, PartyMember caster, PartyMemberSkill skill) {
    if (enemy == null || !enemy.isAlive()) {
      return InterruptResult.notInterrupted();
    }

    // 檢查怪物是否正處於施法蓄力中且允許被打斷
    if (!enemy.isCasting() || !enemy.isInterruptible()) {
      return InterruptResult.notInterrupted();
    }

    // 檢查技能是否具備打斷反制標籤 (STUN / SILENCE / CONTROL / SEAL / PARRY / BREAK / INTERRUPT)
    if (!canInterrupt(skill)) {
      return InterruptResult.notInterrupted();
    }

    String action = enemy.getCurrentAction();
    if (action == null || action.isBlank()) {
      action = enemy.getCurrentIntentName();
    }
    if (action == null || action.isBlank()) {
      action = "蓄力招式";
    }

    long stunMs = (skill.getStunDurationSeconds() > 0)
        ? skill.getStunDurationSeconds() * 1000L
        : 2000L;

    // 執行打斷反制：重置蓄力、破防失衡、硬直僵直
    boolean success = enemy.triggerInterrupt("【" + skill.getName() + "】反制打斷", stunMs);
    if (!success) {
      return InterruptResult.notInterrupted();
    }

    String casterName = (caster != null) ? caster.getName() : "隊友";
    String enemyName = enemy.getName();
    String logMsg = "\u001B[1;31m⚡【反制打斷!】" + casterName + " 施展【" + skill.getName()
        + "】擊破【" + enemyName + "】的【" + action + "】蓄力！怪物罡氣潰散，陷入破防力竭失衡狀態！\u001B[0m";

    log.debug("Interrupt successful: {} interrupted {}'s {}", casterName, enemyName, action);
    return new InterruptResult(true, action, stunMs, logMsg);
  }

  /**
   * 罡氣破防打斷判定：若怪物在施法蓄力期間罡氣破防歸零 (Poise Broken)，即使普通攻擊亦可觸發中斷
   */
  public InterruptResult resolvePoiseBreakInterrupt(BattleEnemy enemy, PartyMember attacker) {
    if (enemy == null || !enemy.isAlive()) {
      return InterruptResult.notInterrupted();
    }
    // 檢查是否因罡氣破防而剛打斷了施法，或當前正處於施法且破防
    String action = enemy.popLastBrokenCastingAction();
    if (action == null && enemy.isCasting() && enemy.isInterruptible() && enemy.isPoiseBroken()) {
      action = enemy.getCurrentAction();
      enemy.triggerInterrupt("罡氣破防震散", 2500L);
    }
    if (action == null || action.isBlank()) {
      return InterruptResult.notInterrupted();
    }

    String attackerName = (attacker != null) ? attacker.getName() : "隊伍";
    String enemyName = enemy.getName();
    String logMsg = "\u001B[1;31m⚡【罡氣破防打斷!】" + attackerName + " 猛烈攻勢震散【" + enemyName
        + "】護體罡氣！打斷了【" + action + "】施法前搖，陷入嚴重失衡硬直！\u001B[0m";

    log.debug("Poise break interrupt successful: {} interrupted {}'s {}", attackerName, enemyName, action);
    return new InterruptResult(true, action, 2500L, logMsg);
  }
}
