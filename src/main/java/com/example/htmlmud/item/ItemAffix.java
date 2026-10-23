package com.example.htmlmud.item;

import java.io.Serializable;
import lombok.Builder;

/**
 * 純單機 Hack-and-slash DRPG 隨機裝備詞綴 (Prefix / Suffix)
 * 對齊 Level 2 ITEM_SCHEMA 規格手冊。
 */
@Builder(toBuilder = true)
public record ItemAffix(
    String id,
    AffixType type,
    String name,
    String statKey,
    double value
) implements Serializable {

  public enum AffixType {
    PREFIX, // 前綴 (例如: "超頻透鏡之", "破軍之", "淬毒的")
    SUFFIX  // 後綴 (例如: "之破魔", "干擾頻譜", "之狂暴")
  }
}
