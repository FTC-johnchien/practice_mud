package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.enums.WeaponType;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;

class WeaponSkillBindingTest {

  @Test
  @DisplayName("驗證武器普攻動態綁定：空手拳腳、裝備長劍切換劍法、自訂主修掛載與卸除武器回歸")
  void testWeaponSkillBindingAndStanceSwitch() {
    PartyMember member = PartyMember.builder()
        .id("m-test")
        .name("劍修道友")
        .row(RowPosition.FRONT)
        .build();

    // 1. 初始空手狀態
    assertThat(member.getEquippedWeapon()).isNull();
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_fist");

    // 2. 穿戴長劍
    PartyItemSlot sword = PartyItemSlot.builder()
        .itemId("taiyin_tomb:bronze_sword")
        .name("青銅古劍")
        .itemType(ItemType.WEAPON)
        .subType(WeaponType.SWORD.name())
        .equipSlot(EquipmentSlot.MAIN_HAND)
        .bonusMinDamage(14)
        .bonusMaxDamage(22)
        .build();
    member.equip(EquipmentSlot.MAIN_HAND, sword);

    assertThat(member.getEquippedWeapon()).isNotNull();
    assertThat(member.getEquippedWeapon().getName()).isEqualTo("青銅古劍");
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_sword");

    // 3. 玩家主動啟用自訂劍法 (例如天劍訣)
    member.enableSkill(SkillCategory.SWORD, "tianjian_sword");
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("tianjian_sword");

    // 4. 卸下武器變回空手 -> 自動回退至空手套路 (basic_fist)
    member.unequip(EquipmentSlot.MAIN_HAND);
    assertThat(member.getEquippedWeapon()).isNull();
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("basic_fist");

    // 5. 再次穿戴長劍 -> 依然記憶之前啟用的天劍訣
    member.equip(EquipmentSlot.MAIN_HAND, sword);
    assertThat(member.getEffectiveBasicSkillId()).isEqualTo("tianjian_sword");
  }
}