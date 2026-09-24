package com.example.htmlmud.domain.port;

import java.util.List;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.GameItem;

/**
 * 領域實體與容器工廠輸出埠口 (Output Port)
 * 遵循依賴反轉原則 (Dependency Inversion Principle)，由 Application 層的 WorldFactory 實作。
 */
public interface WorldEntityFactoryPort {

  /**
   * 建立房間實體 Actor
   */
  Room createRoom(String roomId);

  /**
   * 建立怪物實體 Actor
   */
  Mob createMob(String templateId);

  /**
   * 建立物品實體
   */
  GameItem createItem(String templateId);

  /**
   * 建立生靈死亡之屍體道具
   */
  GameItem createCorpse(Living actor, String killerName);

  /**
   * 抽選怪物掉落物列表
   */
  List<GameItem> generateMobDrops(Mob mob);

  /**
   * 建立擊殺戰利品儲物袋 / 寶箱
   */
  GameItem createLootPouch(Mob mob, List<GameItem> drops);
}
