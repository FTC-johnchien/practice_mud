package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
class DataDrivenPartyAndThreatTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private DrpgBattleService battleService;

  private Party party;

  @BeforeEach
  void setUp() {
    party = partyService.createInitialParty("李長生");
  }

  @Test
  @DisplayName("測試 Data-Driven: 成功從 JSON 載入技能與同伴模板")
  void testDataDrivenTemplatesLoaded() {
    // 1. 驗證 Party 專屬技能已由 JSON 載入至 TemplateRepository
    assertTrue(TemplateRepository.findPartySkill("sword_pierce").isPresent(), "破空劍氣應已從 JSON 載入");
    assertTrue(TemplateRepository.findPartySkill("tank_taunt").isPresent(), "金剛怒目應已從 JSON 載入");
    assertTrue(TemplateRepository.findPartySkill("heal_single").isPresent(), "九轉回春應已從 JSON 載入");

    PartyMemberSkill tauntSkill = TemplateRepository.findPartySkill("tank_taunt").get();
    assertTrue(tauntSkill.isTaunt(), "金剛怒目應具備嘲諷屬性");

    // 2. 驗證同伴模板已由 JSON 載入至 TemplateRepository
    assertTrue(TemplateRepository.findCompanion("iron").isPresent(), "鐵牛模板應已從 JSON 載入");
    assertTrue(TemplateRepository.findCompanion("yan").isPresent(), "燕青模板應已從 JSON 載入");
    assertTrue(TemplateRepository.findCompanion("ling").isPresent(), "凌霜模板應已從 JSON 載入");
    assertTrue(TemplateRepository.findCompanion("mo").isPresent(), "墨衍模板應已從 JSON 載入");
  }

  @Test
  @DisplayName("測試 5 人黃金編制與前後衛職責分派")
  void testFiveManPartyComposition() {
    assertEquals(5, party.size(), "小隊應嚴格限制為 5 人黃金編制");
    assertEquals(5, Party.MAX_PARTY_SIZE, "Party.MAX_PARTY_SIZE 應為 5");

    PartyMember leader = party.getMember(0);
    PartyMember tank = party.getMember(1);
    PartyMember dps = party.getMember(2);
    PartyMember healer = party.getMember(3);
    PartyMember support = party.getMember(4);

    assertEquals("李長生", leader.getName());
    assertEquals(RowPosition.FRONT, leader.getRow(), "天劍主角站前衛");

    assertEquals("鐵牛", tank.getName());
    assertEquals(RowPosition.FRONT, tank.getRow(), "玄甲體修站前衛 (坦克)");

    assertEquals("燕青", dps.getName());
    assertEquals(RowPosition.FRONT, dps.getRow(), "追魂遊俠站前衛");

    assertEquals("凌霜", healer.getName());
    assertEquals(RowPosition.BACK, healer.getRow(), "百草丹修站後衛 (治療)");

    assertEquals("墨衍", support.getName());
    assertEquals(RowPosition.BACK, support.getRow(), "幽冥符修站後衛 (控場)");
  }

  @Test
  @DisplayName("測試仇恨累積與前排坦克嘲諷吸怪機制")
  void testThreatAccumulationAndTaunt() {
    PartyMember leader = party.getMember(0);
    PartyMember tank = party.getMember(1);
    PartyMember healer = party.getMember(3);

    // 初始所有人仇恨為 0
    party.getMembers().forEach(PartyMember::resetThreat);
    assertEquals(0, leader.getThreat());
    assertEquals(0, tank.getThreat());
    assertEquals(0, healer.getThreat());

    BattleEnemy dummyEnemy = BattleEnemy.builder()
        .id("mob-test")
        .name("試煉傀儡")
        .hp(500)
        .maxHp(500)
        .minDamage(10)
        .maxDamage(15)
        .row(RowPosition.FRONT)
        .alive(true)
        .build();

    BattleContext ctx = BattleContext.builder()
        .battleId("test-battle")
        .playerId("李長生")
        .party(party)
        .enemies(List.of(dummyEnemy))
        .build();

    // 1. 主角造成 50 傷害 -> 累積 50 仇恨
    leader.addThreat(50);
    assertEquals(50, leader.getThreat());

    // 2. 丹修施展全體治療 -> 累積 20 仇恨
    healer.addThreat(20);
    assertEquals(20, healer.getThreat());

    // 3. 坦克發動「金剛怒目」嘲諷 -> 激增 600 仇恨並設定 5 秒嘲諷時間戳
    tank.addThreat(600);
    ctx.setTaunt(tank.getId(), 5000);
    assertEquals(600, tank.getThreat());
    assertTrue(ctx.isTaunted(), "當前戰鬥應處於嘲諷狀態");

    // 4. 驗證敵人目標選擇邏輯：反射調用 selectPartyTarget
    try {
      java.lang.reflect.Method selectTargetMethod = DrpgBattleService.class.getDeclaredMethod("selectPartyTarget", BattleContext.class);
      selectTargetMethod.setAccessible(true);

      // (A) 嘲諷期間：敵人必定攻擊嘲諷者 (鐵牛)
      PartyMember targetedDuringTaunt = (PartyMember) selectTargetMethod.invoke(battleService, ctx);
      assertNotNull(targetedDuringTaunt);
      assertEquals("鐵牛", targetedDuringTaunt.getName(), "嘲諷期間怪物應強制攻擊坦克鐵牛");

      // (B) 嘲諷結束後：即使沒有強制鎖定，鐵牛高達 600 * 1.3 = 780 的有效仇恨遠超主角與治療
      ctx.setTauntedUntil(0); // 模擬嘲諷過期
      assertFalse(ctx.isTaunted(), "嘲諷已過期");

      PartyMember targetedAfterTaunt = (PartyMember) selectTargetMethod.invoke(battleService, ctx);
      assertNotNull(targetedAfterTaunt);
      assertEquals("鐵牛", targetedAfterTaunt.getName(), "嘲諷結束後，怪物仍應攻擊最高有效仇恨的坦克");

      // (C) OT 測試：若主角爆發超高傷害 (新增 1000 仇恨，總計 1050 * 1.3 = 1365 > 780)
      leader.addThreat(1000);
      PartyMember targetedAfterOT = (PartyMember) selectTargetMethod.invoke(battleService, ctx);
      assertNotNull(targetedAfterOT);
      assertEquals("李長生", targetedAfterOT.getName(), "當輸出位仇恨超越坦克時發生 OT，怪物轉頭攻擊主角");

    } catch (Exception e) {
      fail("反射測試 selectPartyTarget 失敗: " + e.getMessage());
    }
  }
}
