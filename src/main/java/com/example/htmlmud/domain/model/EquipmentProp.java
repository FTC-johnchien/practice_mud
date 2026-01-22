package com.example.htmlmud.domain.model;

public record EquipmentProp(

    EquipmentSlot slot, // 裝備位置

    String attackVer, // 攻擊動詞: "咬", "抓", "揮拳"

    int damage, // 基礎傷害

    int defense, // 基礎防禦

    int weight, // 重量

    int attackSpeed, // 攻速

    int maxDurability // 最大耐久 100

) {
}
