package com.example.htmlmud.domain.event;

import java.time.Instant;
import java.util.Map;

public sealed interface MobEvents extends DomainEvent permits MobEvents.MobDead {
  // 怪物死亡
  record MobDead(String mobId, String killerId, // 誰殺的
      Map<String, Double> dropItems, // 掉落物預覽
      Instant occurredOn) implements MobEvents {
  }
}
