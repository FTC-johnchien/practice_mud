package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;

/**
 * 一次擲骰圓桌判定 (One-Roll Combat Table) 專屬單元測試
 * 測試圓桌順序：[Miss] -> [Dodge] -> [Parry] -> [Block] -> [Crit] -> [Normal Hit]
 */
@SpringBootTest
public class OneRollCombatTableTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private DefenseResolver defenseResolver;

  private PartyMember defender;
  private BattleEnemy attacker;

  @BeforeEach
  void setUp() {
    Party party = partyService.createFullParty("table_tester");
    defender = party.getMembers().get(0);
    defender.getEnabledSkills().clear();
    defender.getEquipment().clear();
    defender.getStats().setDex(30);
    defender.getStats().setStr(30);
    defender.getStats().setCon(30);

    attacker = BattleEnemy.builder()
        .id("test_enemy")
        .name("幽冥斥候")
        .hp(100)
        .maxHp(100)
        .minDamage(20)
        .maxDamage(40)
        .defense(5)
        .dex(20)
        .build();
  }

  @Test
  @DisplayName("圓桌切片 1: 擲骰落於 Miss 區間，應結算為 MISS，0 傷害且日誌標記未命中")
  void testCombatTableMissSlice() {
    // 預定擲骰極小值 0.01，必定落於 Miss 區間 (5%+)
    var res = defenseResolver.resolveEnemyAttack(attacker, defender, 30, "爪擊", 0.01);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.MISS);
    assertThat(res.finalDamage()).isEqualTo(0);
    assertThat(res.spGained()).isEqualTo(0);
    assertThat(res.combatLog()).contains("💨【未命中】");
  }

  @Test
  @DisplayName("圓桌切片 2: 配備身法技能且擲骰落於 Dodge 區間，應結算為 DODGED，0 傷害且獎勵 15 SP")
  void testCombatTableDodgeSlice() {
    defender.enableSkill(SkillCategory.DODGE, "cloud_step");
    // missChance 約 0.05 + (30-20)*0.005 = 0.10
    // dodgeChance 約 0.10 + (30*0.008) - (20*0.003) = 0.28
    // dodge 範圍在 0.10 ~ 0.38 之間，選定 0.20
    var res = defenseResolver.resolveEnemyAttack(attacker, defender, 40, "突刺", 0.20);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.DODGED);
    assertThat(res.finalDamage()).isEqualTo(0);
    assertThat(res.spGained()).isEqualTo(15);
    assertThat(res.combatLog()).contains("💨【身法閃避】");
  }

  @Test
  @DisplayName("圓桌切片 3: 配備招架技能且擲骰落於 Parry 區間，應結算為 PARRIED，傷害減半且獎勵 SP 與怒氣")
  void testCombatTableParrySlice() {
    defender.enableSkill(SkillCategory.PARRY, "iron_cloth");
    // 不配置 Dodge，missChance 約 0.10, dodge=0
    // parryChance 約 0.15 + (30*0.004) + (30*0.004) = 0.39
    // parry 範圍在 0.10 ~ 0.49 之間，選定 0.25
    var res = defenseResolver.resolveEnemyAttack(attacker, defender, 50, "橫斬", 0.25);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.PARRIED);
    assertThat(res.finalDamage()).isLessThanOrEqualTo(25);
    assertThat(res.finalDamage()).isGreaterThan(0);
    assertThat(res.spGained()).isEqualTo(5);
    assertThat(res.combatLog()).contains("🛡️【招架格擋】");
  }

  @Test
  @DisplayName("圓桌切片 4: 副手配備盾牌且擲骰落於 Block 區間，應結算為 BLOCKED，扣除盾牌格擋值")
  void testCombatTableShieldBlockSlice() {
    // 裝備盾牌
    PartyItemSlot shield = PartyItemSlot.builder()
        .slotId("shield_01")
        .itemId("iron_shield")
        .name("玄鐵重盾")
        .itemType(ItemType.SHIELD)
        .equipSlot(EquipmentSlot.OFF_HAND)
        .bonusDefense(15)
        .build();
    defender.equipShield(shield);

    // 不配置 Dodge 與 Parry，missChance 約 0.10
    // blockChance 約 0.20 + 30*0.003 = 0.29
    // block 範圍在 0.10 ~ 0.39 之間，選定 0.22
    int rawDamage = 40;
    var res = defenseResolver.resolveEnemyAttack(attacker, defender, rawDamage, "重砸", 0.22);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.BLOCKED);
    // 格擋值 = 15 * 2 + 30 / 2 = 30 + 15 = 45 -> 傷害被完全抵消至 min 1
    assertThat(res.finalDamage()).isEqualTo(1);
    assertThat(res.combatLog()).contains("🛡️【盾牌格擋】").contains("玄鐵重盾");
  }

  @Test
  @DisplayName("圓桌切片 5: 擲骰落於 Crit 區間，應結算為 CRIT，承受 1.5 倍爆擊重創")
  void testCombatTableCritSlice() {
    // 未裝備身法、招架、盾牌，missChance 約 0.10
    // critChance 約 0.05 + 20*0.002 = 0.09
    // crit 範圍在 0.10 ~ 0.19 之間，選定 0.15
    int rawDamage = 30;
    var res = defenseResolver.resolveEnemyAttack(attacker, defender, rawDamage, "致命破甲", 0.15);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.CRIT);
    assertThat(res.finalDamage()).isEqualTo((int) Math.round(30 * 1.5));
    assertThat(res.rageGained()).isEqualTo(15);
    assertThat(res.combatLog()).contains("💥【致命一擊】");
  }

  @Test
  @DisplayName("圓桌切片 6: 擲骰落於其餘常態區間，應結算為普通 HIT，承受正常扣防傷害")
  void testCombatTableNormalHitSlice() {
    // 擲骰 0.90，落於最外圍的 Normal Hit 區間
    int rawDamage = 25;
    var res = defenseResolver.resolveEnemyAttack(attacker, defender, rawDamage, "普通平砍", 0.90);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.HIT);
    assertThat(res.finalDamage()).isEqualTo(25);
    assertThat(res.combatLog()).contains("⚡【幽冥斥候】");
  }
}
