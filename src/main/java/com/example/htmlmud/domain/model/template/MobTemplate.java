package com.example.htmlmud.domain.model.template;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import com.example.htmlmud.domain.model.config.LootEntry;
import com.example.htmlmud.domain.model.enums.Gender;
import com.example.htmlmud.domain.model.enums.MobKind;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;

@Builder(toBuilder = true)
public record MobTemplate(

    String id,

    String name,

    String race,

    Gender gender,

    List<String> aliases,

    MobKind kind,

    int level,

    int maxHp,

    int maxMp,

    int maxStamina,

    // int maxSan,

    String description,

    String lookDescription,

    int expReward, // 死亡給多少經驗

    boolean isAggressive, // 是否主動攻擊

    boolean isInvincible, // 是否無敵

    List<String> dialogues, // 預設對話庫
    // 掉落表 ID, 商店列表 ID...
    Integer shopId,

    int str, // 力量
    @JsonAlias("int") int intelligence, // 智力
    int dex, // 靈巧
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

    String behavior, // 行為

    Map<String, String> equipment, // 装備

    Map<SkillCategory, String> enabledSkills,

    // 掉落表：列表中的每個項目代表一種可能的掉落物
    List<LootEntry> loot

) {
  public MobTemplate {
    if (lookDescription == null) {
      lookDescription = description;
    }
    if (gender == null) {
      gender = Gender.ANIMAL;
    }
    if (kind == null) {
      kind = MobKind.NEUTRAL;
    }
    if (str == 0) {
      str = 5;
    }
    if (intelligence == 0) {
      intelligence = 5;
    }
    if (dex == 0) {
      dex = 5;
    }
    if (con == 0) {
      con = 5;
    }
    if (maxStamina == 0) {
      maxStamina = 100;
    }
    if (attackSpeed == 0) {
      attackSpeed = 2000;
    }
    if (equipment == null) {
      equipment = Map.of();
    }
    if (loot == null) {
      loot = List.of();
    }
  }

  public String getRandomDialogue() {
    if (dialogues == null || dialogues.isEmpty()) {
      return null;
    }
    // 使用 ThreadLocalRandom 效率最高
    int index = ThreadLocalRandom.current().nextInt(dialogues.size());
    return dialogues.get(index);
  }
}
