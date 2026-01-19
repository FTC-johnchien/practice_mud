package com.example.htmlmud.domain.model;

import java.util.List;
import com.example.htmlmud.domain.model.json.LivingState;

// 這是在 Actor 之間傳遞的快照 (Snapshot)
public record PlayerRecord(

    String id,

    String name,

    String displayName,

    Integer currentRoomId,

    // 注意：如果是 Record，這裡最好是 Deep Copy 後的資料
    LivingState state,

    List<GameItem> inventory

) {
}
