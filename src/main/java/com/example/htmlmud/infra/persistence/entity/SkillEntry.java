package com.example.htmlmud.infra.persistence.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SkillEntry {
  private String skillId;
  private int level;
  private long xp; // 當前熟練度
  private long maxXp; // 升級所需熟練度 (通常由公式計算，不一定存檔)
  private boolean temporary;

  // 建構子

  public SkillEntry(String id) {
    this(id, 0);
  }

  public SkillEntry(String id, int level) {
    this(id, level, false);
  }

  private SkillEntry(String id, int level, boolean temporary) {
    this.skillId = id;
    this.level = level;
    this.xp = 0; // 歸0計算？
    this.temporary = temporary;
  }

  public void addXp(long amount) {
    this.xp += amount;
  }

  public void levelUp() {
    this.level++;
    this.xp = 0;
  }

  public static SkillEntry createMobSkillEntry(String id, int level) {
    return new SkillEntry(id, level, true);
  }

  // public boolean isTemporary() {
  // return (temporary || this.level == 0);
  // }

}
