package com.example.htmlmud.party;

/**
 * 戰術 Gambit 規則領域模型 (GambitRule)
 * 借鑒 Final Fantasy XII 戰術體系，為小隊同伴提供基於優先順序的條件-動作觸發鏈。
 */
public class GambitRule extends TacticsRule {

  public GambitRule() {
    super();
  }

  public GambitRule(int priority, TacticsCondition condition, int conditionValue,
                    TacticsTarget target, String skillId, String itemId,
                    GambitActionType actionType, boolean enabled) {
    super(priority, condition, conditionValue, target, skillId, itemId, actionType, enabled);
  }

  /**
   * 建立反制打斷專用 Gambit 規則：當敵方處於可打斷吟唱前搖時，施放打斷招式
   */
  public static TacticsRule ofInterrupt(int priority, String skillId) {
    return TacticsRule.builder()
        .priority(priority)
        .condition(TacticsCondition.ENEMY_CASTING_INTERRUPTIBLE)
        .conditionValue(0)
        .target(TacticsTarget.CASTING_ENEMY)
        .skillId(skillId)
        .actionType(GambitActionType.SKILL)
        .enabled(true)
        .build();
  }

  /**
   * 建立隊友急救治療 Gambit 規則：當隊友氣血低於閾值時，對其施放治療術
   */
  public static TacticsRule ofHeal(int priority, int hpThreshold, String skillId) {
    return TacticsRule.builder()
        .priority(priority)
        .condition(TacticsCondition.ALLY_HP_LESS_THAN)
        .conditionValue(hpThreshold)
        .target(TacticsTarget.LOWEST_HP_ALLY)
        .skillId(skillId)
        .actionType(GambitActionType.SKILL)
        .enabled(true)
        .build();
  }

  /**
   * 建立道心安神 Gambit 規則：當隊友道心 SAN 值低於閾值時，施放清心法術
   */
  public static TacticsRule ofEmergencySan(int priority, int sanThreshold, String skillId) {
    return TacticsRule.builder()
        .priority(priority)
        .condition(TacticsCondition.ALLY_SAN_LESS_THAN)
        .conditionValue(sanThreshold)
        .target(TacticsTarget.LOWEST_HP_ALLY)
        .skillId(skillId)
        .actionType(GambitActionType.SKILL)
        .enabled(true)
        .build();
  }

  /**
   * 建立破防趁虛攻堅 Gambit 規則：當敵怪處於破防失衡狀態時，施展高爆發技能
   */
  public static TacticsRule ofPoiseBrokenBurst(int priority, String skillId) {
    return TacticsRule.builder()
        .priority(priority)
        .condition(TacticsCondition.ENEMY_POISE_BROKEN)
        .conditionValue(0)
        .target(TacticsTarget.POISE_BROKEN_ENEMY)
        .skillId(skillId)
        .actionType(GambitActionType.SKILL)
        .enabled(true)
        .build();
  }
}
