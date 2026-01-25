package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.example.htmlmud.domain.model.LootEntry;
import com.example.htmlmud.domain.model.MobKind;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;

@Builder(toBuilder = true)
public record MobTemplate(

    String id,

    String name,

    List<String> aliases,

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

    int str, // 力量
    @JsonAlias("int") int intelligence, // 智力
    int agi, // 敏捷
    int con, // 體質

    // === 天生攻擊定義 (Natural Attack) ===
    // 如果手上沒武器，就用這個設定
    String attackVerb, // 攻擊動詞: "咬", "抓", "揮拳"
    String attackNoun, // 攻擊部位: "尖銳的牙齒", "利爪", "拳頭"
    int minDamage, // 最小傷害
    int maxDamage, // 最大傷害
    int hitRate, // 天生命中率
    int defense, // 天生防禦力
    int attackSpeed, // 天生攻速
    int weight, // 天生重量

    Map<String, String> equipment, // 装備

    // 掉落表：列表中的每個項目代表一種可能的掉落物
    List<LootEntry> lootTable

) {
}
