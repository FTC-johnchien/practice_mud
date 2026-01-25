package com.example.htmlmud.domain.model.vo;

public record DamageSource(

    String name, // 來源名稱 (ex: "生鏽鐵劍", "巨大的門牙")
    String verb, // 動詞 (ex: "揮砍", "咬")
    int minDamage, // 最小傷害
    int maxDamage, // 最大傷害
    int attackSpeed, // 攻速 (毫秒)
    int hitRate, // 命中加成
    int durability // 耐久度
) {

  public static final DamageSource DEFAULT_FIST = new DamageSource("拳頭", "揮擊", 1, 3, 2000, 0, -1);
}
