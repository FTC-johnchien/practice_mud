package com.example.htmlmud.domain.model.enums;

/**
 * 引擎底層結算行為分流：
 * - SHIELD: 傷害吸收護盾
 * - HOT: 週期性治療 (Heal over Time)
 * - DOT: 週期性傷害 (Damage over Time)
 * - STAT_MODIFIER: 屬性百分比或固定值修正
 * - CONTROL: 強制控場限制 (如暈眩、定身、混亂)
 */
public enum BuffCategory {
  SHIELD,
  HOT,
  DOT,
  STAT_MODIFIER,
  CONTROL
}
