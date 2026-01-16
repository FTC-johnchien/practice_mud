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

// // 存放地上的物品 (Item ID -> ItemRecord)
// private final Long2ObjectOpenHashMap<ItemRecord> itemsOnFloor = new Long2ObjectOpenHashMap<>();

// public RoomActor(RoomRecord record) {
// super("room-" + record.id());
// this.roomId = record.id();
// this.exits = record.exits();
// }

// // ... handleMessage 邏輯 ...
// }
