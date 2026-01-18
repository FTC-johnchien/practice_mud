package com.example.htmlmud.domain.model;

import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;

// 這是在 Actor 之間傳遞的快照 (Snapshot)
public record PlayerRecord(String id, String name, String displayName, Integer currentRoomId,
    LivingState state // 注意：如果是 Record，這裡最好是 Deep Copy 後的資料
) {
  // 提供一個靜態工廠方法從 Entity 轉換
  // public static PlayerRecord from(PlayerEntity e) {
  // return new PlayerRecord(e.id, e.name, e.displayName, e.currentRoomId, e.state // 實務上建議 copy 一份
  // // state 防止副作用
  // );
  // }

}
