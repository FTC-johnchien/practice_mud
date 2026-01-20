package com.example.htmlmud.domain.model.map;

import java.util.Set;
import com.example.htmlmud.domain.model.MobKind;
import lombok.Builder;

@Builder(toBuilder = true)
public record MobTemplate(

    String id,

    String name,

    String[] aliases,

    MobKind kind,

    int level,

    int maxHp,

    int damage,

    String roomDescription,

    String lookDescription,

    int expReward, // 死亡給多少經驗

    boolean isAggressive, // 是否主動攻擊

    boolean isInvincible, // 是否無敵

    Set<String> dialogues, // 預設對話庫
    // 掉落表 ID, 商店列表 ID...
    Integer shopId) {
}
