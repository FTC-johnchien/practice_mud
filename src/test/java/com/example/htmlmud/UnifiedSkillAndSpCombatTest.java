package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BattleState;
import com.example.htmlmud.domain.dungeon.battle.ComboResolver;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.config.Costs;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.model.enums.SkillType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class UnifiedSkillAndSpCombatTest {

  @Autowired
  private TemplateRepository templateRepository;

  @Autowired
  private PartyService partyService;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private DrpgBattleService battleService;

  @Autowired
  private ComboResolver comboResolver;

  @Autowired
  private DrpgEnemyTacticsService tacticsService;

  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    assertNotNull(templateRepository);
    assertNotNull(comboResolver);
  }

  @Test
  @DisplayName("1. 驗證 PartyMember SP 資源積累與舊版 Rage/Combo 別名相容性")
  void testPartyMemberSpResourceAndAliasCompatibility() {
    PartyMember member = PartyMember.builder()
        .id("test-sp-member")
        .name("凌雲劍客")
        .currentSp(0)
        .maxSp(100)
        .build();

    assertEquals(0, member.getCurrentSp());
    assertEquals(0, member.getCurrentRage());
    assertEquals(0, member.getCurrentCombo());

    // 獲得 30 點 SP
    member.gainSp(30);
    assertEquals(30, member.getCurrentSp());
    assertEquals(30, member.getCurrentRage());
    assertEquals(1, member.getCurrentCombo()); // 30 / 20 = 1

    // 消耗 20 點 SP
    boolean consumed = member.consumeSp(20);
    assertTrue(consumed);
    assertEquals(10, member.getCurrentSp());
    assertEquals(10, member.getCurrentRage());
    assertEquals(0, member.getCurrentCombo());

    // 透過舊版 gainRage 獲得 40 點
    member.gainRage(40);
    assertEquals(50, member.getCurrentSp());
    assertEquals(50, member.getCurrentRage());
    assertEquals(2, member.getCurrentCombo());

    // 超額扣除防禦
    assertFalse(member.consumeSp(100));
    assertEquals(50, member.getCurrentSp());
  }

  @Test
  @DisplayName("2. 驗證 Costs 與 SkillType 對 SP、COMBO、FORMATION 的解析")
  void testCostsAndSkillTypeJsonParsing() throws Exception {
    String costsJson = "{\"sp\": 35, \"mp\": 10, \"hp\": 0}";
    Costs costs = objectMapper.readValue(costsJson, Costs.class);
    assertEquals(35, costs.sp());
    assertEquals(10, costs.mp());
    assertEquals(0, costs.hp());

    // 測試 SkillType 新增列舉值
    assertEquals("多人合擊", SkillType.COMBO.getDescription());
    assertEquals("陣法絕技", SkillType.FORMATION.getDescription());
  }

  @Test
  @DisplayName("3. 驗證 PartyMemberSkill 分類推斷與多重消耗載入")
  void testPartyMemberSkillCategoryAndCosts() {
    var tauntTpl = templateRepository.getSkillTemplate("class_warrior_taunt").orElse(null);
    assertNotNull(tauntTpl);
    PartyMemberSkill tauntSkill = PartyMemberSkill.fromSkillTemplate(tauntTpl);
    assertNotNull(tauntSkill);
    assertEquals("CLASS", tauntSkill.getCategory());
    assertTrue(tauntSkill.getSpCost() > 0 || tauntSkill.getCostValue() > 0);

    var swordTpl = templateRepository.getSkillTemplate("sword_pierce").orElse(null);
    assertNotNull(swordTpl);
    PartyMemberSkill swordSkill = PartyMemberSkill.fromSkillTemplate(swordTpl);
    assertNotNull(swordSkill);
    assertEquals("WEAPON", swordSkill.getCategory());

    var healTpl = templateRepository.getSkillTemplate("class_cleric_heal").orElse(null);
    assertNotNull(healTpl);
    PartyMemberSkill healSkill = PartyMemberSkill.fromSkillTemplate(healTpl);
    assertNotNull(healSkill);
    assertEquals("SPELL", healSkill.getCategory());
  }

  @Test
  @DisplayName("4. 驗證 DrpgEnemyTacticsService canCastSkill 與 consumeSkillResource 支援 SP")
  void testTacticsServiceSupportSp() {
    PartyMember member = PartyMember.builder()
        .id("tactics-member")
        .name("戰術隊員")
        .currentSp(25)
        .build();

    PartyMemberSkill cheapSkill = PartyMemberSkill.builder()
        .id("skill-cheap")
        .costType(ResourceType.SP)
        .costValue(20)
        .build();

    PartyMemberSkill expensiveSkill = PartyMemberSkill.builder()
        .id("skill-expensive")
        .costType(ResourceType.SP)
        .costValue(50)
        .build();

    assertTrue(tacticsService.canCastSkill(member, cheapSkill));
    assertFalse(tacticsService.canCastSkill(member, expensiveSkill));

    assertTrue(tacticsService.consumeSkillResource(member, cheapSkill));
    assertEquals(5, member.getCurrentSp());
  }

  @Test
  @DisplayName("5. 驗證 ComboResolver 多人合擊條件判斷與執行")
  void testComboResolverExecution() {
    PartyMember warrior = PartyMember.builder()
        .id("m-warrior")
        .name("重甲狂徒")
        .classId("WARRIOR")
        .alive(true)
        .currentSp(50)
        .build();

    PartyMember rogue = PartyMember.builder()
        .id("m-rogue")
        .name("魅影刺客")
        .classId("ROGUE")
        .alive(true)
        .currentSp(40)
        .build();

    Party party = Party.builder()
        .partyName("合擊戰隊")
        .members(new ArrayList<>(List.of(warrior, rogue)))
        .formationEnergy(60)
        .build();

    BattleEnemy enemy = BattleEnemy.builder()
        .id("enemy-boss")
        .name("黑煞統領")
        .hp(500)
        .maxHp(500)
        .alive(true)
        .build();

    BattleContext ctx = BattleContext.builder()
        .battleId("combo-battle-test")
        .playerId("test-player")
        .party(party)
        .enemies(new ArrayList<>(List.of(enemy)))
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    // 取得刀劍合璧技能
    PartyMemberSkill comboSkill = templateRepository.getPartySkill("combo_blade_and_shadow").orElse(null);
    assertNotNull(comboSkill, "combo_blade_and_shadow 必須存在於 party_skills.json");
    assertTrue(comboSkill.isSynergy());

    // 條件滿足，可執行合擊
    assertTrue(comboResolver.canExecuteCombo(ctx, comboSkill));

    // 執行合擊
    List<String> logs = new ArrayList<>();
    boolean executed = comboResolver.executeCombo(null, ctx, comboSkill, (p, msg) -> logs.add(msg));
    assertTrue(executed);

    // 扣除各參與者資源
    assertTrue(warrior.getCurrentSp() < 50, "戰士應扣除 SP");
    assertTrue(rogue.getCurrentSp() < 40, "盜賊應扣除 SP");
    assertTrue(enemy.getHp() < 500, "敵怪應受到重創");
    assertFalse(logs.isEmpty(), "應產生小隊合擊廣播日誌");

    // 驗證失控狀態下排除合擊資格
    warrior.setMadnessState(PartyMember.MadnessState.CHAOS);
    assertFalse(comboResolver.canExecuteCombo(ctx, comboSkill), "隊員走火入魔 (CHAOS) 時不得參與合擊");
  }
}
