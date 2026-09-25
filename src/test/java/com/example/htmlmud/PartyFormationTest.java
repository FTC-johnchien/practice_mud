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
    assertEquals(5, party.size(), "初始應有滿編 5 人（2 前衛、1 中衛、2 後衛）");
    assertEquals("李長生", party.getMember(0).getName());
    assertEquals(RowPosition.FRONT, party.getMember(0).getRow(), "主角為前衛");
    assertEquals(RowPosition.FRONT, party.getMember(1).getRow(), "鐵牛為前衛");
    assertEquals(RowPosition.MIDDLE, party.getMember(2).getRow(), "燕青為中衛");
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

  @Test
  @DisplayName("測試 FormationEngine：隊員陣亡即時觸發破陣、清除 Buff 與靈威")
  void testFormationEngineBreakOnMemberDeath() {
    var formationEngine = new com.example.htmlmud.domain.party.service.FormationEngine();
    var fiveElements = partyService.getFormation("formation_five_elements");
    assertNotNull(fiveElements);

    formationEngine.applyFormation(party, fiveElements);
    assertFalse(party.isFormationBroken());
    party.setFormationEnergy(100);
    assertTrue(party.canCastUltimate());

    // 隊員 #3 (index 2) 陣亡
    PartyMember victim = party.getMembers().get(2);
    victim.setAlive(false);

    // 觸發破陣檢驗
    String breakMsg = formationEngine.checkAndBreakFormation(party, victim.getName() + " 力竭倒下");
    assertNotNull(breakMsg, "減員應產生破陣提示");
    assertTrue(breakMsg.contains("陣法崩解"));
    assertTrue(party.isFormationBroken(), "隊伍陣法應被標記為崩解");
    assertEquals(0, party.getFormationEnergy(), "靈威應清零");
    assertFalse(party.canCastUltimate(), "崩解後無法施展奧義");

    // 全員陣法 Buff 應已被移除
    for (PartyMember m : party.getMembers()) {
      for (int i = 0; i < 5; i++) {
        assertFalse(m.hasActiveBuff("buff_formation_slot_" + i), "破陣後陣法 Buff 應被清除");
      }
    }
  }

  @Test
  @DisplayName("測試 FormationEngine：存活成員自動排位映射 (Auto-mapping) 與換陣止血")
  void testFormationEngineAutoMappingOnSwitchToFourPersonFormation() {
    var formationEngine = new com.example.htmlmud.domain.party.service.FormationEngine();
    var fourSymbols = partyService.getFormation("formation_four_symbols");
    assertNotNull(fourSymbols);

    // 5 人隊伍中，第 3 位 (index 2) 陣亡，存活人數為 4
    party.getMembers().get(2).setAlive(false);
    assertEquals(4, party.getAliveCount());
    assertEquals(5, party.size());
    assertEquals(4, formationEngine.getEffectivePartySize(party));

    // 換成 4 人陣法
    assertTrue(formationEngine.isFormationEligible(party, fourSymbols));
    formationEngine.applyFormation(party, fourSymbols);
    assertFalse(party.isFormationBroken());

    // 驗證 Auto-mapping 映射：
    // index 0 -> slot 0
    // index 1 -> slot 1
    // index 2 (陣亡) -> null
    // index 3 -> slot 2
    // index 4 -> slot 3
    var slot0 = party.getSlotForMember(0);
    var slot1 = party.getSlotForMember(1);
    var slot2Dead = party.getSlotForMember(2);
    var slot3 = party.getSlotForMember(3);
    var slot4 = party.getSlotForMember(4);

    assertNotNull(slot0);
    assertEquals(0, slot0.getSlotIndex());

    assertNotNull(slot1);
    assertEquals(1, slot1.getSlotIndex());

    assertNull(slot2Dead, "陣亡成員不應分派孔位");

    assertNotNull(slot3);
    assertEquals(2, slot3.getSlotIndex(), "活著的第 3 位成員應自動遞補到孔位 2");

    assertNotNull(slot4);
    assertEquals(3, slot4.getSlotIndex(), "活著的第 4 位成員應自動遞補到孔位 3");

    // 驗證數值倍率有正確套用
    var stats3 = party.calculateEffectiveStats(3);
    assertNotNull(stats3);
    assertEquals((int) Math.round(party.getMembers().get(3).getBaseMinDamage() * slot3.getAttackMultiplier()), stats3.minDamage());
  }

  @Test
  @DisplayName("測試 5 人陣法完整性與 5x3 浪漫沙加座標配置")
  void testFivePersonFormationsAndGridCoords() {
    // 1. 驗證鳳天舞之陣 (朱雀)
    var phoenix = partyService.getFormation("formation_phoenix");
    assertNotNull(phoenix);
    assertEquals("鳳天舞之陣", phoenix.getName());
    assertEquals(5, phoenix.getSlots().size());
    // 帝王中央陣眼 slot 2: gridX=2, gridY=1
    assertEquals(2, phoenix.getSlots().get(2).getGridX());
    assertEquals(1, phoenix.getSlots().get(2).getGridY());

    // 2. 驗證十字陣 (純物攻/純防禦浪漫沙加軍陣)
    var cross = partyService.getFormation("formation_cross");
    assertNotNull(cross);
    assertEquals("十字陣", cross.getName());
    // 先鋒 (2,0), 左翼 (1,1), 陣心 (2,1), 右翼 (3,1), 殿後 (2,2)
    assertEquals(2, cross.getSlots().get(0).getGridX());
    assertEquals(0, cross.getSlots().get(0).getGridY());
    assertEquals(1, cross.getSlots().get(1).getGridX());
    assertEquals(1, cross.getSlots().get(1).getGridY());
    assertEquals(2, cross.getSlots().get(2).getGridX());
    assertEquals(1, cross.getSlots().get(2).getGridY());
    assertEquals(3, cross.getSlots().get(3).getGridX());
    assertEquals(1, cross.getSlots().get(3).getGridY());
    assertEquals(2, cross.getSlots().get(4).getGridX());
    assertEquals(2, cross.getSlots().get(4).getGridY());

    // 3. 驗證青龍騰雲陣 (青龍)
    var qingLong = partyService.getFormation("formation_qing_long");
    assertNotNull(qingLong);
    assertEquals("青龍騰雲陣", qingLong.getName());
    assertTrue(qingLong.getRequiredClasses().contains("ROGUE"));

    // 4. 驗證白虎嘯日陣 (白虎)
    var baiHu = partyService.getFormation("formation_bai_hu");
    assertNotNull(baiHu);
    assertEquals("白虎嘯日陣", baiHu.getName());
    assertTrue(baiHu.getRequiredClasses().contains("WARRIOR"));

    // 5. 驗證玄武鎮嶽陣 (玄武)
    var xuanWu = partyService.getFormation("formation_xuan_wu");
    assertNotNull(xuanWu);
    assertEquals("玄武鎮嶽陣", xuanWu.getName());
    assertTrue(xuanWu.getRequiredClasses().contains("WARRIOR"));

    // 6. 總共應有 7 種 5 人陣法
    var f5List = partyService.getFormationsForPartySize(5);
    assertEquals(7, f5List.size(), "五行、十字、鳳天舞、青龍、白虎、玄武、玄陰");
  }
}
