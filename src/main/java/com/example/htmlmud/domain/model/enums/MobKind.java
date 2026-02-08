package com.example.htmlmud.domain.model.enums;

public enum MobKind {
  FRIENDLY, // 友善 (村長, 商人)
  NEUTRAL, // 中立 (鹿, 黃牛 - 被打才會反擊)
  AGGRESSIVE, // 主動攻擊 (哥布林, 狼)
  BOSS // 首領 (顯示時可能會有特殊顏色)
}
