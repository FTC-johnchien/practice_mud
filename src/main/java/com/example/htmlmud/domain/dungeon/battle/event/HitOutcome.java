package com.example.htmlmud.domain.dungeon.battle.event;

public enum HitOutcome {
  HIT,      // 普通命中受創
  CRIT,     // 致命一擊 (暴擊受創)
  BLOCKED,  // 盾牌格擋成功 (扣除盾牌格擋值)
  PARRIED,  // 招架格擋成功 (減免 50%~70%)
  DODGED,   // 身法閃避成功 (0 傷害)
  MISS,     // 未命中 (0 傷害)
  ABSORBED, // 護盾全額吸收
  IMMUNE    // 免疫
}
