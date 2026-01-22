package com.example.htmlmud.domain.model.vo;

public record DamageSource(

    String name, // 來源名稱 (ex: "生鏽鐵劍", "巨大的門牙")
    String verb, // 動詞 (ex: "揮砍", "咬")
    int damage, // 數值
    int speed // 攻速

) {
}
