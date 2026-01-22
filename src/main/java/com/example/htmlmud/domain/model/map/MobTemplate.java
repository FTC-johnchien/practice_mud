package com.example.htmlmud.domain.model.map;

import java.util.Map;
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

    int maxMp,

    int maxStamina,

    int maxSan,

    String roomDescription,

    String lookDescription,

    int expReward, // 死亡給多少經驗

    boolean isAggressive, // 是否主動攻擊

    boolean isInvincible, // 是否無敵

    Set<String> dialogues, // 預設對話庫
    // 掉落表 ID, 商店列表 ID...
    Integer shopId,

    // === 天生攻擊定義 (Natural Attack) ===
    // 如果手上沒武器，就用這個設定
    String attackVerb, // 攻擊動詞: "咬", "抓", "揮拳"
    String attackNoun, // 攻擊部位: "尖銳的牙齒", "利爪", "拳頭"
    int baseDamage, // 天生基礎傷害
    int baseDefense, // 天生防禦力
    int attackSpeed, // 天生攻速
    int weight, // 天生重量

    Map<String, String> equipment // 装備

) {
}
