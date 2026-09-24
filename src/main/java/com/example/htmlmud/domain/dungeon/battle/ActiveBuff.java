package com.example.htmlmud.domain.dungeon.battle;

import java.util.HashMap;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import com.example.htmlmud.domain.model.enums.EffectType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 戰鬥實體掛載的單一作用中 Buff / Debuff 狀態實體。
 * 遵循基準心跳計數器模型（1 Tick = 500ms），數值可精準預測、可存檔且 100% 易於測試。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActiveBuff {

  private String id;                    // 狀態唯一識別碼 (如: buff_gold_shield)
  private String sourceSkillId;         // 來源招式 ID (如: class_cleric_bless)
  private String sourceCasterId;        // 施法者 ID (如: m-ling_shuang)
  private String name;                  // 狀態名稱 (如: 金光辟邪護體)
  @Builder.Default
  private String icon = "✨";           // 前端小圖標 (如: 🛡️, 🌿, 🧪, ⚡)
  @Builder.Default
  private BuffType type = BuffType.BUFF;
  @Builder.Default
  private BuffCategory category = BuffCategory.SHIELD;
  private EffectType effectType;        // 語意類型標籤 (如: SHIELD, POISON, STUNED)

  // 生命週期：心跳計數 (1 Tick = 500ms)
  private int durationTicks;            // 原始總持續 Ticks (如 20s = 40 ticks)
  private int remainingTicks;           // 剩餘持續 Ticks

  // 週期性跳算 (Tick Interval)
  private int tickIntervalTicks;        // 跳算週期 (如 2s 一跳 = 4 ticks, 0 代表不跳算)
  @Builder.Default
  private int ticksSinceLastTick = 0;   // 距上次跳算的計數累加

  // 數值與堆疊
  private int value;                    // 當前容量 (護盾剩餘吸收值、每跳治療量或傷害量)
  @Builder.Default
  private int stacks = 1;               // 當前層數
  @Builder.Default
  private int maxStacks = 1;            // 最大層數

  // 屬性百分比或固定值修正 (如: "DEFENSE": 0.15, "MAX_HP": 0.10)
  @Builder.Default
  private Map<String, Double> statModifiers = new HashMap<>();

  @JsonIgnore
  public boolean isExpired() {
    return remainingTicks <= 0 || (category == BuffCategory.SHIELD && value <= 0);
  }

  /**
   * 每個心跳計數遞減一次
   */
  public void decrementTick() {
    if (this.remainingTicks > 0) {
      this.remainingTicks--;
    }
    this.ticksSinceLastTick++;
  }

  /**
   * 是否達到下一次週期跳算觸發門檻
   */
  @JsonIgnore
  public boolean shouldTickNow() {
    if (tickIntervalTicks <= 0) return false;
    return ticksSinceLastTick >= tickIntervalTicks;
  }

  /**
   * 重置跳算週期計數
   */
  public void resetTickCounter() {
    this.ticksSinceLastTick = 0;
  }

  /**
   * 前端顯示用：換算剩餘秒數 (向上取整)
   */
  @JsonIgnore
  public int getRemainingSeconds() {
    return (int) Math.ceil(remainingTicks * 0.5);
  }
}
