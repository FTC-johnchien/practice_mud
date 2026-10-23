package com.example.htmlmud.domain.model.entity;

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

  /**
   * 轉換為大一統規範技能實體 (PartyMemberSkill)
   */
  public com.example.htmlmud.domain.party.model.PartyMemberSkill toUnifiedSkill(
      com.example.htmlmud.template.TemplateReader reader) {
    if (reader == null) {
      return com.example.htmlmud.domain.party.model.PartyMemberSkill.builder()
          .id(this.skillId)
          .name(this.skillId)
          .level(Math.max(1, this.level))
          .xp(this.xp)
          .maxXp(this.maxXp > 0 ? this.maxXp : 100L)
          .build();
    }
    // 優先查詢 PartySkill 模板
    var partyOpt = reader.findPartySkill(this.skillId);
    if (partyOpt.isPresent()) {
      var skill = partyOpt.get();
      skill.setLevel(Math.max(1, this.level));
      skill.setXp(this.xp);
      if (this.maxXp > 0) skill.setMaxXp(this.maxXp);
      return skill;
    }
    // 查不到時查 MUD SkillTemplate 並自動適配
    var mudOpt = reader.findSkill(this.skillId);
    if (mudOpt.isPresent()) {
      var skill = com.example.htmlmud.domain.party.model.PartyMemberSkill.fromSkillTemplate(mudOpt.get());
      if (skill != null) {
        skill.setLevel(Math.max(1, this.level));
        skill.setXp(this.xp);
        if (this.maxXp > 0) skill.setMaxXp(this.maxXp);
        return skill;
      }
    }
    return com.example.htmlmud.domain.party.model.PartyMemberSkill.builder()
        .id(this.skillId)
        .name(this.skillId)
        .level(Math.max(1, this.level))
        .xp(this.xp)
        .maxXp(this.maxXp > 0 ? this.maxXp : 100L)
        .build();
  }

  /**
   * 自大一統規範技能反向同步為 SkillEntry
   */
  public static SkillEntry fromUnifiedSkill(com.example.htmlmud.domain.party.model.PartyMemberSkill skill) {
    if (skill == null) return null;
    SkillEntry entry = new SkillEntry(skill.getId(), Math.max(1, skill.getLevel()));
    entry.setXp(skill.getXp());
    entry.setMaxXp(skill.getMaxXp());
    return entry;
  }

  public double getEffectiveDamageMultiplier(double baseMultiplier) {
    int lvl = Math.max(1, this.level);
    double raw = baseMultiplier * (1.0 + (lvl - 1) * 0.05);
    return Math.round((raw + 1e-9) * 100.0) / 100.0;
  }

  public double getProficiencyProgress() {
    if (maxXp <= 0) return 0.0;
    return Math.min(1.0, Math.max(0.0, (double) xp / maxXp));
  }
}
