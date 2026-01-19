package com.example.htmlmud.domain.model.map;

import java.util.List;
import com.example.htmlmud.domain.model.MobKind;

public record MobTemplate(

    int id,

    String name,

    String[] aliases,

    MobKind kind,

    String roomDescription,

    String lookDescription,

    int maxHp,

    int level,

    int expReward, // 死亡給多少經驗

    boolean isAggressive, // 是否主動攻擊

    boolean isInvincible, // 是否無敵

    List<String> dialogues, // 預設對話庫
    // 掉落表 ID, 商店列表 ID...
    Integer shopId) {
}
