package com.example.htmlmud.domain.actor;

import com.example.htmlmud.domain.actor.core.VirtualActor;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class RoomActor {

}
// public class RoomActor extends VirtualActor<RoomMessage> {

// private final long roomId;
// private final RoomExitsData exits;

// // 【FastUtil 運用】
// // 使用 Long2ObjectMap 取代 HashMap<Long, PlayerActor>
// // 雖然 PlayerActor 是物件，但 Key 是 primitive long，這能避免大量 Long 物件的拆裝箱
// private final Long2ObjectOpenHashMap<PlayerActor> playersInRoom = new Long2ObjectOpenHashMap<>();

// 使用 Composite ID 作為 Key
// 這樣 Player 1 和 Mob 1 就不會衝突了！
// private final Map<GameObjectId, LivingActor> inhabitants = new HashMap<>();

// // 存放地上的物品 (Item ID -> ItemRecord)
// private final Long2ObjectOpenHashMap<ItemRecord> itemsOnFloor = new Long2ObjectOpenHashMap<>();

// public RoomActor(RoomRecord record) {
// super("room-" + record.id());
// this.roomId = record.id();
// this.exits = record.exits();
// }

// // ... handleMessage 邏輯 ...
// private void handleAttack(GameObjectId attackerId, GameObjectId targetId) {
// LivingActor target = inhabitants.get(targetId);
// if (target != null) {
// // 多型 (Polymorphism) 的威力：
// // 不管是 Player 還是 Mob，都呼叫同一個方法
// target.takeDamage(10, attackerId);
// }
// }
// }
