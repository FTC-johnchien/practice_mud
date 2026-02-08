package com.example.htmlmud.domain.model.entity;

import java.util.List;

// 這是在 Actor 之間傳遞的快照 (Snapshot)
public record PlayerRecord(

    String id,

    String name,

    String nickname,

    String currentRoomId,

    // 注意：如果是 Record，這裡最好是 Deep Copy 後的資料
    LivingStats stats,

    List<GameItem> inventory

) {
}
