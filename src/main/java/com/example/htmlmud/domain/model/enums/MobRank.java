package com.example.htmlmud.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;

/**
 * 生靈戰鬥階級 (Mob Rank)
 * 與陣營態度 (MobKind) 正交分離，決定數值加成、視覺標籤與掉落權重。
 */
@Getter
public enum MobRank {

  /** 普通大眾生靈 / 雜役村民 / 野生動物 */
  @JsonAlias({"normal", "NORMAL", "common", "COMMON"})
  NORMAL("普通", "", 1.0, 1.0),

  /** 精英生靈 (屬性強化、金色標籤、專屬稀有掉落) */
  @JsonAlias({"elite", "ELITE"})
  ELITE("精英", "【精英】", 1.5, 1.2),

  /** 區域首領 / 副本王 (霸氣標籤、大血條、免疫控制、特殊全域掉落) */
  @JsonAlias({"boss", "BOSS", "leader", "LEADER"})
  BOSS("首領", "【首領】", 3.0, 1.5);

  private final String displayName;
  private final String prefix;
  private final double hpMultiplier;
  private final double damageMultiplier;

  MobRank(String displayName, String prefix, double hpMultiplier, double damageMultiplier) {
    this.displayName = displayName;
    this.prefix = prefix;
    this.hpMultiplier = hpMultiplier;
    this.damageMultiplier = damageMultiplier;
  }
}
