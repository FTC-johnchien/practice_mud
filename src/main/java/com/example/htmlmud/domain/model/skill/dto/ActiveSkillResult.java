package com.example.htmlmud.domain.model.skill.dto;

import com.example.htmlmud.domain.model.map.SkillTemplate;
import com.example.htmlmud.infra.persistence.entity.SkillEntry;

/**
 * 封裝「當前正在使用的技能」之完整資訊 包含靜態設定 (Template) 與 動態等級 (Entry)
 */
public record ActiveSkillResult(SkillTemplate template, // 技能的原始設定 (JSON)
    SkillEntry entry // 玩家的修練狀態 (Level, XP)
) {
  // 您可以在這裡加一些便利方法 (Helper Methods)

  /**
   * 取得當前技能等級 (如果 entry 是 null，代表是臨時領悟的 Lv 1)
   */
  public int getLevel() {
    return (entry != null) ? entry.getLevel() : 1;
  }

  /**
   * 判斷這是不是一個還沒真正學會的臨時技能
   */
  public boolean isTemporary() {
    return entry == null || entry.isTemporary();
  }

  public SkillTemplate getTemplate() {
    return template;
  }

  public SkillEntry getEntry() {
    return entry;
  }

}
