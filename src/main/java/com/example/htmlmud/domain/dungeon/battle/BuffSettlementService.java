package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.config.BuffConfig;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 DRPG 狀態效果（Buff / Debuff / Shield / HoT / DoT）之生命週期結算、堆疊/刷新判定及傷害護盾吸收。
 * 遵循基準心跳計數器模型（1 Tick = 500ms），達成 100% 可預測且純粹測試。
 */
@Slf4j
@Service
public class BuffSettlementService {

  /**
   * 結算目標實體上所有狀態經過 1 個 Tick（500ms）之變化。
   * 包括：持續時間遞減、HoT 週期治療、DoT 週期傷害、過期與破碎狀態移除。
   *
   * @param target 受結算之戰鬥實體 (PartyMember 或 BattleEnemy)
   * @return 本次跳算產生的戰鬥日誌清單
   */
  public List<String> processTicks(Buffable target) {
    List<String> eventLogs = new ArrayList<>();
    if (target == null || !target.isAlive()) {
      return eventLogs;
    }

    List<ActiveBuff> buffs = target.getActiveBuffs();
    if (buffs == null || buffs.isEmpty()) {
      return eventLogs;
    }

    List<ActiveBuff> snapshot = new ArrayList<>(buffs);
    List<ActiveBuff> expiredBuffs = new ArrayList<>();

    for (ActiveBuff buff : snapshot) {
      buff.decrementTick();

      // 1. 週期性跳算檢定 (HoT / DoT)
      if (buff.shouldTickNow()) {
        buff.resetTickCounter();
        if (buff.getCategory() == BuffCategory.HOT) {
          int healAmt = buff.getValue() * Math.max(1, buff.getStacks());
          int oldHp = target.getHp();
          int newHp = Math.min(target.getMaxHp(), oldHp + healAmt);
          target.setHp(newHp);
          int actualHealed = newHp - oldHp;
          eventLogs.add("\u001B[1;32m" + buff.getIcon() + "【" + buff.getName() + "】靈息流轉，為 "
              + target.getName() + " 恢復 " + actualHealed + " 點氣血！\u001B[0m");
        } else if (buff.getCategory() == BuffCategory.DOT) {
          int dmg = buff.getValue() * Math.max(1, buff.getStacks());
          target.takeDamage(dmg);
          eventLogs.add("\u001B[1;35m" + buff.getIcon() + "【" + buff.getName() + "】劇毒侵蝕，對 "
              + target.getName() + " 造成 " + dmg + " 點陰煞傷害！\u001B[0m");
          if (!target.isAlive()) {
            eventLogs.add("\u001B[1;31m💀 " + target.getName() + " 不敵【" + buff.getName() + "】毒發身亡！\u001B[0m");
            break;
          }
        }
      }

      // 2. 過期判定 (時間結束或護盾歸零)
      if (buff.isExpired()) {
        expiredBuffs.add(buff);
      }
    }

    // 移除過期 Buff 並廣播失效通知
    for (ActiveBuff expired : expiredBuffs) {
      buffs.remove(expired);
      if (expired.getCategory() == BuffCategory.SHIELD && expired.getValue() <= 0) {
        eventLogs.add("🛡️【" + expired.getName() + "】靈光耗盡，護盾破碎化為光屑！");
      } else {
        eventLogs.add("⌛ " + target.getName() + " 身上的【" + expired.getName() + "】效果已消退。");
      }
    }

    return eventLogs;
  }

  /**
   * 根據技能的 BuffConfig 與受法目標，生成並施加新的 ActiveBuff 狀態。
   */
  public ActiveBuff createActiveBuffFromConfig(BuffConfig cfg, Buffable target, String casterId, String skillId) {
    if (cfg == null || target == null) return null;

    int durationSeconds = cfg.durationSeconds() > 0 ? cfg.durationSeconds() : 10;
    int durationTicks = durationSeconds * 2; // 1s = 2 ticks

    int tickIntervalSeconds = cfg.tickIntervalSeconds();
    int tickIntervalTicks = tickIntervalSeconds > 0 ? tickIntervalSeconds * 2 : 0;

    int val = 0;
    if (cfg.category() == BuffCategory.SHIELD) {
      int base = cfg.baseShield() > 0 ? cfg.baseShield() : 30;
      double pct = cfg.percentOfTargetHp() > 0 ? cfg.percentOfTargetHp() : 0.0;
      val = Math.max(base, (int) (target.getMaxHp() * pct));
    } else if (cfg.category() == BuffCategory.HOT) {
      int baseHeal = cfg.healPerTick() > 0 ? cfg.healPerTick() : 15;
      double pctHeal = cfg.healPercentPerTick() > 0 ? cfg.healPercentPerTick() : 0.0;
      val = Math.max(baseHeal, (int) (target.getMaxHp() * pctHeal));
    } else if (cfg.category() == BuffCategory.DOT) {
      val = cfg.damagePerTick() > 0 ? cfg.damagePerTick() : 12;
    }

    return ActiveBuff.builder()
        .id(cfg.id() != null ? cfg.id() : ("buff_" + skillId))
        .sourceSkillId(skillId)
        .sourceCasterId(casterId)
        .name(cfg.name() != null ? cfg.name() : "未知狀態")
        .icon(cfg.icon() != null ? cfg.icon() : "✨")
        .type(cfg.type() != null ? cfg.type() : BuffType.BUFF)
        .category(cfg.category() != null ? cfg.category() : BuffCategory.SHIELD)
        .effectType(cfg.effectType())
        .durationTicks(durationTicks)
        .remainingTicks(durationTicks)
        .tickIntervalTicks(tickIntervalTicks)
        .ticksSinceLastTick(0)
        .value(val)
        .stacks(1)
        .maxStacks(cfg.maxStacks() > 0 ? cfg.maxStacks() : 1)
        .statModifiers(cfg.statModifiers() != null ? cfg.statModifiers() : java.util.Map.of())
        .build();
  }

  /**
   * 施加狀態至目標實體（委派至 target.addBuff，滿足 WoW 同名刷新時間、不同名共存疊加原則）
   */
  public void applyBuff(Buffable target, ActiveBuff buff) {
    if (target != null && buff != null) {
      target.addBuff(buff);
    }
  }
}
