package com.example.htmlmud.domain.model.enums;

public enum SkillCategory {
  // === 基礎戰鬥位 (根據手持武器自動切換) ===
  UNARMED, // 空手時
  SWORD, // 持劍時
  BLADE, // 持刀時
  DAGGER, // 持匕首時
  POLEARM, // 持長柄武器時
  SPEAR, // 持長槍時
  STAFF, // 持棍時
  HAMMER, // 持锤時
  AXE, // 持斧時
  WHIP, // 持鞭時
  WAND, // 持法杖時
  BOW, // 持弓時
  // ... 其他武器類型

  // === 核心戰鬥位 (常駐) ===
  DODGE, // 輕功 (決定閃避率 & 描述)
  PARRY, // 招架 (決定格擋率 & 描述)
  FORCE, // 內功 (決定回血/大招)

  // === 生活/輔助位 ===
  MEDICAL, // 醫術
  MAGIC // 魔法
}
