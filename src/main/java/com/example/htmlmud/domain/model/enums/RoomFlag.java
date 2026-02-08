package com.example.htmlmud.domain.model.enums;

public enum RoomFlag {

  // 環境類 flags
  // 室內。不受天氣系統（如降雨、打雷）影響，且通常不能在此使用騎乘寵物。
  INDOORS,
  // 戶外
  OUTDOORS,
  // 永久黑暗。除非玩家持有光源（如火把、提燈），否則無法看見房間描述與出口。
  DARK,
  // 水下。玩家若沒有呼吸藥水或相關技能，會持續受傷甚至溺斃。
  UNDERWATER,


  // 戰鬥與安全 flags
  // 安全區 可以自動回復
  SAFE_ZONE,
  // 不可以PVP
  NO_PVP,
  // 競技場 允許PVP
  ARENA,
  // 禁止怪物 (MOBs) 進入。確保該區域只有玩家，避免怪物追擊到特定安全區域。
  NO_MOB,
  // 死亡陷阱。一旦進入該房間，玩家會立即死亡，通常帶有警示性描述。
  DEATH_TRAP,


  // 限制類 flags
  // 禁止使用「回城」指令（如 recall）。通常用於副本、監獄或特殊任務區域，防止玩家在危險時瞬間逃脫。
  NO_RECALL,
  // 禁止傳送類魔法（如 teleport 或 summon）進入或離開該房間。常用於保護特定區域的完整性。
  NO_TELEPORT,
  // 禁止使用召喚術將玩家或怪物傳送到此房間，或從此處召喚走。
  NO_SUMMON,
  // 禁魔區。在該房間內無法施展任何法術，常用於教堂、市集或特定解謎房間。
  NO_MAGIC,


  // 特殊功能 flags
  // 高恢復區。在該房間休息（rest 或 sleep）時，生命值與法力值的恢復速度加倍。
  HIGH_REGEN,
  // 私人房間。同一時間只允許兩個人進入，常用於秘密對話或角色扮演。
  PRIVATE,
  // 銀行。在此可以使用存提款指令。
  BANK,
  // 商店。在此可以向 NPC 買賣物品。
  SHOP,

  // ：高空掉落。如果房間沒有對應的出口，玩家會直接摔落到下方的房間並受傷。
  FALL

}
