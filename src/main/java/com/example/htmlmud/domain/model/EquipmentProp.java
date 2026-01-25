package com.example.htmlmud.domain.model;

import com.example.htmlmud.domain.model.vo.DamageSource;
import lombok.Builder;

@Builder(toBuilder = true)
public record EquipmentProp(

    // @JsonProperty("slot") @JsonFormat(
    // with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<EquipmentSlot> slots, // 裝備位置
    EquipmentSlot slot, // 裝備位置

    String name, // 武器名稱

    String attackVer, // 攻擊動詞: "咬", "抓", "揮拳"

    int minDamage, // 最小傷害

    int maxDamage, // 最大傷害

    int defense, // 基礎防禦

    int weight, // 重量

    int attackSpeed, // 攻速

    int hitRate, // 命中率

    int range, // 攻擊範圍

    int critRate, // 暴擊率 %

    int critDamage, // 暴擊傷害 %

    int maxDurability // 最大耐久 -1 代表無限


) {

  public DamageSource getDamageSource(String name) {
    return new DamageSource(name, this.attackVer, this.minDamage, this.maxDamage, this.attackSpeed,
        this.attackSpeed, this.maxDurability);
  }
}
