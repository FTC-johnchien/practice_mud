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
    assertEquals(5, party.size(), "初始應有滿編 5 人（3 前衛、2 後衛）");
    assertEquals("李長生", party.getMember(0).getName());
    assertEquals(RowPosition.FRONT, party.getMember(0).getRow(), "主角為前衛");
    assertEquals(RowPosition.FRONT, party.getMember(1).getRow(), "鐵牛為前衛");
    assertEquals(RowPosition.FRONT, party.getMember(2).getRow(), "燕青為前衛");
    assertEquals(RowPosition.BACK, party.getMember(3).getRow(), "凌霜為後衛");
    assertEquals(RowPosition.BACK, party.getMember(4).getRow(), "墨衍為後衛");
    assertNotNull(party.getEquippedFormation(), "應預設裝備《四象辟邪陣》");
  }

  @Test
  @DisplayName("測試小隊上限 5 人限制")
  void testPartyMaxCap() {
    // 初始已是滿編 5 人
    assertEquals(5, party.size(), "初始應已達 5 人滿編");

    // 嘗試加入第 6 人應被拒絕
    boolean sixthAdded = party.addMember(PartyMember.builder()
        .id("m-6")
        .name("道友6")
        .roleTitle("散修")
        .row(RowPosition.BACK)
        .stats(new LivingStats())
        .build());
    assertFalse(sixthAdded, "超過 5 人上限應拒絕加入");
    assertEquals(5, party.size());
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

  @Test
  @DisplayName("測試不同小隊人數之陣法匹配與基本陣法")
  void testDynamicFormationsByPartySize() {
    // 2人隊伍
    var f2 = partyService.getFormationsForPartySize(2);
    assertEquals(1, f2.size());
    assertEquals("formation_liang_yi", f2.get(0).getId());
    assertEquals("兩儀微塵陣", f2.get(0).getName());
    assertEquals("formation_liang_yi", partyService.getBasicFormationForPartySize(2).getId());

    // 3人隊伍
    var f3 = partyService.getFormationsForPartySize(3);
    assertEquals(1, f3.size());
    assertEquals("formation_san_cai", f3.get(0).getId());
    assertEquals("三才聚靈陣", f3.get(0).getName());
    assertEquals("formation_san_cai", partyService.getBasicFormationForPartySize(3).getId());

    // 4人隊伍
    var f4 = partyService.getFormationsForPartySize(4);
    assertEquals(1, f4.size());
    assertEquals("formation_four_symbols", f4.get(0).getId());
    assertEquals("四象辟邪陣", f4.get(0).getName());
    assertEquals("formation_four_symbols", partyService.getBasicFormationForPartySize(4).getId());

    // 5人隊伍
    var f5 = partyService.getFormationsForPartySize(5);
    assertTrue(f5.size() >= 4, "5人隊伍應有五行、十字、鳳凰、玄陰陣");
    assertEquals("formation_five_elements", partyService.getBasicFormationForPartySize(5).getId());
  }

  @Test
  @DisplayName("測試進階陣法職業限制（鳳凰陣需戰/法/牧）")
  void testFormationClassRequirements() {
    var phoenix = partyService.getFormation("formation_phoenix");
    assertNotNull(phoenix);
    assertTrue(phoenix.getRequiredClasses().contains("WARRIOR"));
    assertTrue(phoenix.getRequiredClasses().contains("MAGE"));
    assertTrue(phoenix.getRequiredClasses().contains("CLERIC"));

    // 滿編隊伍：李長生(SWORDSMAN), 鐵牛(WARRIOR), 燕青(ROGUE), 凌霜(CLERIC), 墨衍(MAGE)
    // 擁有 戰(鐵牛)、法(墨衍)、牧(凌霜)，因此符合鳳凰陣條件
    assertTrue(phoenix.isEligibleForParty(party), "滿編隊伍具備戰法牧，應符合鳳凰陣要求");
    assertTrue(phoenix.checkClassRequirements(party).isEmpty());

    // 移除法師墨衍，替換為另一名劍修
    party.getMembers().remove(4);
    party.getMembers().add(PartyMember.builder()
        .id("m-extra")
        .name("純劍修")
        .classId("SWORDSMAN")
        .roleTitle("劍修")
        .row(RowPosition.BACK)
        .stats(new LivingStats())
        .build());

    // 此時缺少法師(MAGE)
    assertFalse(phoenix.isEligibleForParty(party), "缺少符修(法)應不符合鳳凰陣");
    var missing = phoenix.checkClassRequirements(party);
    assertEquals(1, missing.size());
    assertTrue(missing.get(0).contains("法") || missing.get(0).contains("MAGE"));
  }

  @Test
  @DisplayName("測試更換隊員或人數異動時，陣法自動校驗與退回該人數基本陣法")
  void testPartyMemberChangeFallback() {
    // 1. 滿編時裝備進階 5 人陣法「鳳凰陣」
    var phoenix = partyService.getFormation("formation_phoenix");
    party.setEquippedFormation(phoenix);
    assertEquals("formation_phoenix", party.getEquippedFormation().getId());

    // 2. 遣散一名隊員使得隊伍人數降為 4 人
    party.getMembers().remove(party.getMembers().size() - 1);
    assertEquals(4, party.size());

    // 3. 校驗並重組陣法
    String notice = party.validateAndAlignFormation(partyService);
    assertNotNull(notice, "人數變更應產生降級提示");
    assertTrue(notice.contains("四象辟邪陣"), "4人隊伍應自動退回4人基本陣法《四象辟邪陣》");
    assertEquals("formation_four_symbols", party.getEquippedFormation().getId());

    // 4. 再遣散一名隊員使得人數降為 3 人
    party.getMembers().remove(party.getMembers().size() - 1);
    assertEquals(3, party.size());
    notice = party.validateAndAlignFormation(partyService);
    assertEquals("formation_san_cai", party.getEquippedFormation().getId(), "3人隊伍應退回《三才聚靈陣》");

    // 5. 再遣散一名隊員使得人數降為 2 人
    party.getMembers().remove(party.getMembers().size() - 1);
    assertEquals(2, party.size());
    notice = party.validateAndAlignFormation(partyService);
    assertEquals("formation_liang_yi", party.getEquippedFormation().getId(), "2人隊伍應退回《兩儀微塵陣》");
  }

  @Test
  @DisplayName("測試客棧初始可招募同伴包含4人（鐵牛、凌霜、燕青、墨衍）")
  void testTownInnHasFourRecruitableCompanions() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var is = getClass().getClassLoader().getResourceAsStream("data/zones/newbie_village/rooms.json")) {
      assertNotNull(is, "newbie_village/rooms.json 應存在");
      java.util.List<com.example.htmlmud.domain.model.template.RoomTemplate> rooms =
          mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      var innOpt = rooms.stream().filter(r -> "inn".equals(r.id())).findFirst();
      assertTrue(innOpt.isPresent(), "客棧房間應存在");
      var spawns = innOpt.get().spawnRules();
      assertNotNull(spawns);
      var mobIds = spawns.stream().map(com.example.htmlmud.domain.model.template.SpawnRule::id).toList();
      assertTrue(mobIds.contains("tie_niu"), "客棧應可招募鐵牛");
      assertTrue(mobIds.contains("ling_shuang"), "客棧應可招募凌霜");
      assertTrue(mobIds.contains("yan_qing"), "客棧應可招募燕青");
      assertTrue(mobIds.contains("mo_yan"), "客棧應可招募墨衍");
    }
  }
}
