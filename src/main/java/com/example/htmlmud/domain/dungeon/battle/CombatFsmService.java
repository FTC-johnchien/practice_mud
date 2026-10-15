package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
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
    if (enemy == null || !enemy.isAlive()) return;
    enemy.startIntent(icon, actionName, type, scope, durationMs, interruptible);
    log.debug("Monster [{}] entered CASTING state for [{}] (duration={}ms, interruptible={})",
        enemy.getName(), actionName, durationMs, interruptible);
  }

  /**
   * 檢定技能是否具備打斷反制能力 (包含 STUN、SILENCE、CONTROL、SEAL 標籤或 stun 屬性)
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
            || "SEAL".equalsIgnoreCase(tag)) {
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

    // 檢查技能是否具備打斷反制標籤 (STUN / SILENCE / CONTROL / SEAL)
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
}
