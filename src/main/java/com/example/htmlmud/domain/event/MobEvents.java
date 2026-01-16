package com.example.htmlmud.domain.event;

import java.time.Instant;
import java.util.Map;
import com.example.htmlmud.domain.model.GameObjectId;

public sealed interface MobEvents extends DomainEvent permits MobEvents.MobDead {
  // 怪物死亡
  record MobDead(GameObjectId mobId, GameObjectId killerId, // 誰殺的
      Map<String, Double> dropItems, // 掉落物預覽
      Instant occurredOn) implements MobEvents {
  }
}
