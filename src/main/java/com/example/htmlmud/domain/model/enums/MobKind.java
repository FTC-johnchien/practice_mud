package com.example.htmlmud.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 生靈互動態度與立場 (Mob Kind / Alignment)
 * 專注於陣營立場與 AI 主被動行為，與戰鬥階級 (MobRank) 正交。
 */
public enum MobKind {
  /** 友善 NPC (村長、商販、招募夥伴) */
  @JsonAlias({"FRIENDLY", "friendly"})
  FRIENDLY,

  /** 中立生靈 (家禽、野鹿、修仙散人 - 被挑釁才會反擊) */
  @JsonAlias({"NEUTRAL", "neutral", "passive", "PASSIVE"})
  NEUTRAL,

  /** 主動敵對生靈 (礦坑魔物、野狼、山賊 - 見人即索敵) */
  @JsonAlias({"AGGRESSIVE", "aggressive", "HOSTILE", "hostile"})
  AGGRESSIVE,

  /**
   * @deprecated 首領階級已遷移至 {@link MobRank#BOSS}，保留此列舉僅供舊存檔與 JSON 向下相容。
   */
  @Deprecated
  @JsonAlias({"BOSS", "boss"})
  BOSS
}
