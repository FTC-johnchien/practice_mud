package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;

class PartyFormationTest {

  private PartyService partyService;
  private Party party;

  @BeforeEach
  void setUp() {
    partyService = new PartyService();
    partyService.initDefaultFormations();
    party = partyService.createInitialParty("李長生");
  }

  @Test
  @DisplayName("測試初始小隊建立與前後衛編制")
  void testInitialPartyCreation() {
    assertEquals(6, party.size(), "初始應有滿編 6 人（3 前衛、3 後衛）");
    assertEquals("李長生", party.getMember(0).getName());
    assertEquals(RowPosition.FRONT, party.getMember(0).getRow(), "主角為前衛");
    assertEquals(RowPosition.FRONT, party.getMember(1).getRow(), "鐵牛為前衛");
    assertEquals(RowPosition.FRONT, party.getMember(2).getRow(), "燕青為前衛");
    assertEquals(RowPosition.BACK, party.getMember(3).getRow(), "凌霜為後衛");
    assertEquals(RowPosition.BACK, party.getMember(4).getRow(), "墨衍為後衛");
    assertEquals(RowPosition.BACK, party.getMember(5).getRow(), "芷若為後衛");
    assertNotNull(party.getEquippedFormation(), "應預設裝備《四象辟邪陣》");
  }

  @Test
  @DisplayName("測試小隊上限 6 人限制")
  void testPartyMaxCap() {
    // 初始已是滿編 6 人
    assertEquals(6, party.size(), "初始應已達 6 人滿編");

    // 嘗試加入第 7 人應被拒絕
    boolean seventhAdded = party.addMember(PartyMember.builder()
        .id("m-7")
        .name("道友7")
        .roleTitle("散修")
        .row(RowPosition.BACK)
        .stats(new LivingStats())
        .build());
    assertFalse(seventhAdded, "超過 6 人上限應拒絕加入");
    assertEquals(6, party.size());
  }

  @Test
  @DisplayName("測試陣位屬性加成計算 (天樞 1 號位 +15% 攻擊, +10% 防禦)")
  void testFormationSlotMultiplier() {
    PartyMember leader = party.getMember(0);
    // 基礎攻擊: 15 ~ 25，基礎防禦: 5
    assertEquals(15, leader.getBaseMinDamage());
    assertEquals(25, leader.getBaseMaxDamage());
    assertEquals(5, leader.getBaseDefense());

    // 天樞陣眼 slotIndex=0: attack x1.15, defense x1.10
    com.example.htmlmud.domain.party.model.EffectiveCombatStats eff = party.calculateEffectiveStats(0);
    assertNotNull(eff);
    assertEquals((int) Math.round(15 * 1.15), eff.minDamage());
    assertEquals((int) Math.round(25 * 1.15), eff.maxDamage());
    assertEquals((int) Math.round(5 * 1.10), eff.defense());
  }

  @Test
  @DisplayName("測試陣法切換與大招施放及克蘇魯 SAN 值代價")
  void testFormationSwitchAndUltimateSanCost() {
    // 1. 切換至禁忌邪陣《玄陰噬魂陣》
    party.setEquippedFormation(partyService.getFormation("formation_xuan_yin"));
    assertEquals("玄陰噬魂陣", party.getEquippedFormation().getName());

    // 2. 初始靈威不足
    party.setFormationEnergy(50);
    assertFalse(party.canCastUltimate(), "50 點靈威不足以釋放大招");

    // 3. 注入靈威至滿
    party.addFormationEnergy(50);
    assertEquals(100, party.getFormationEnergy());
    assertTrue(party.canCastUltimate(), "100 點靈威應可釋放大招");

    // 4. 記錄全員初始 SAN
    int initialLeaderSan = party.getMember(0).getCurrentSan();
    int initialTankSan = party.getMember(1).getCurrentSan();

    // 5. 發動大招《不可名狀星蝕》（sanCost = 8）
    party.consumeFormationEnergy();
    assertEquals(0, party.getFormationEnergy(), "靈威應已耗盡歸零");

    // 6. 驗證全隊道心 SAN 遭侵蝕扣減 8 點
    assertEquals(initialLeaderSan - 8, party.getMember(0).getCurrentSan(), "主角 SAN 應扣減 8 點");
    assertEquals(initialTankSan - 8, party.getMember(1).getCurrentSan(), "鐵牛 SAN 應扣減 8 點");
  }
}
