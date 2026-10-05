package com.example.htmlmud.domain.dungeon.battle.event;

public enum BattleEventType {
  ATTACK,        // 普通物理/生靈攻擊
  SKILL,         // 主動戰技/法術 (傷害)
  HEAL,          // 治療
  SHIELD,        // 辟邪護盾 / 不動金身
  BUFF,          // 增益 / 嘲諷
  CAST_START,    // 開始吟唱
  CAST_INTERRUPT,// 吟唱被打斷
  DOT_TICK,      // 持續傷害跳字
  HOT_TICK,      // 持續治療跳字
  RIPOSTE        // 破招突刺反擊
}
