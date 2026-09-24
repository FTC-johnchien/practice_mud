package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.FormationSlot;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;

@SpringBootTest
@ActiveProfiles("test")
class PartySwapAndAoeDamageTest {

  private DrpgEnemyTacticsService tacticsService;
  private DrpgCombatLoop combatLoop;

  @BeforeEach
  void setUp() {
    tacticsService = new DrpgEnemyTacticsService();
    combatLoop = new DrpgCombatLoop(tacticsService, null);
  }

  private LivingStats createStats(int hp, int mp) {
    LivingStats s = new LivingStats();
    s.setHp(hp);
    s.setMaxHp(hp);
    s.setMp(mp);
    s.setMaxMp(mp);
    return s;
  }

  @Test
  @DisplayName("機制測試 1: 隊員換位 swapMembers 與隊長身份不變性")
  void testPartySwapMembers() {
    PartyMember leader = PartyMember.builder()
        .id("m-leader")
        .name("玄靈子")
        .row(RowPosition.FRONT)
        .build();

    PartyMember warrior = PartyMember.builder()
        .id("m-iron")
        .name("鐵牛")
        .row(RowPosition.FRONT)
        .build();

    PartyMember cleric = PartyMember.builder()
        .id("m-ling")
        .name("凌霜")
        .row(RowPosition.BACK)
        .build();

    Party party = Party.builder()
        .id("test-party")
        .members(new ArrayList<>(List.of(leader, warrior, cleric)))
        .build();

    assertEquals("玄靈子", party.getMembers().get(0).getName());
    assertEquals("鐵牛", party.getMembers().get(1).getName());
    assertEquals("凌霜", party.getMembers().get(2).getName());

    // 換位 0 與 1
    boolean ok = party.swapMembers(0, 1);
    assertTrue(ok);
    assertEquals("鐵牛", party.getMembers().get(0).getName());
    assertEquals("玄靈子", party.getMembers().get(1).getName());

    // 隊長仍能正確以 isLeader 取得
    assertEquals("玄靈子", party.getLeader().getName());

    // 邊界測試：無效 index
    assertFalse(party.swapMembers(-1, 0));
    assertFalse(party.swapMembers(0, 5));
    assertFalse(party.swapMembers(1, 1)); // 相同 index
  }

  @Test
  @DisplayName("機制測試 2: AOE 技能傷害輸出於戰鬥日誌 (太陰萬劍訣)")
  void testAoeDamageCombatLog() {
    PartyMember member = PartyMember.builder()
        .id("m-sword")
        .name("玄靈子")
        .row(RowPosition.FRONT)
        .resourceType(CombatResourceType.SP)
        .stats(createStats(120, 60))
        .baseMinDamage(20)
        .baseMaxDamage(30)
        .build();

    PartyMemberSkill aoeSkill = PartyMemberSkill.builder()
        .id("sword_storm")
        .name("太陰萬劍訣")
        .costType(CombatResourceType.SP)
        .costValue(40)
        .damageMultiplier(1.4)
        .aoe(true)
        .build();

    BattleEnemy enemy1 = BattleEnemy.builder()
        .id("puppet_1")
        .name("玄鐵試道傀儡·乾")
        .row(RowPosition.FRONT)
        .hp(100)
        .maxHp(100)
        .rank(MobRank.NORMAL)
        .alive(true)
        .build();

    BattleEnemy enemy2 = BattleEnemy.builder()
        .id("puppet_2")
        .name("凝霜試法陣樁·坤")
        .row(RowPosition.BACK)
        .hp(100)
        .maxHp(100)
        .rank(MobRank.NORMAL)
        .alive(true)
        .build();

    Party party = Party.builder()
        .id("p-test")
        .members(new ArrayList<>(List.of(member)))
        .build();

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setEnemies(new ArrayList<>(List.of(enemy1, enemy2)));

    // 執行 AOE 技能
    combatLoop.applySkillEffects(null, ctx, member, aoeSkill, -1, "");

    // 驗證敵人血量皆有扣減
    assertTrue(enemy1.getHp() < 100, "敵怪 1 應受到 AOE 傷害");
    assertTrue(enemy2.getHp() < 100, "敵怪 2 應受到 AOE 傷害");

    // 驗證戰鬥日誌包含整體出招宣告與逐隻命中傷害！
    List<String> logs = ctx.getRecentLogs();
    assertNotNull(logs);
    boolean hasCastDeclaration = logs.stream().anyMatch(l -> l.contains("祭出【太陰萬劍訣】") && l.contains("排山倒海的威能橫掃敵方全體"));
    boolean hasEnemy1HitLog = logs.stream().anyMatch(l -> l.contains("擊中【玄鐵試道傀儡·乾】造成") && l.contains("點傷害"));
    boolean hasEnemy2HitLog = logs.stream().anyMatch(l -> l.contains("擊中【凝霜試法陣樁·坤】造成") && l.contains("點傷害"));

    assertTrue(hasCastDeclaration, "戰鬥日誌應有橫掃全場宣告");
    assertTrue(hasEnemy1HitLog, "戰鬥日誌應明確顯示對傀儡·乾的實質傷害點數");
    assertTrue(hasEnemy2HitLog, "戰鬥日誌應明確顯示對陣樁·坤的實質傷害點數");
  }

  @Test
  @DisplayName("機制測試 3: 陣法孔位與站位要求 (FormationSlot)")
  void testFormationSlotMatching() {
    FormationSlot slot0 = FormationSlot.builder()
        .slotIndex(0)
        .slotName("天樞【陣眼主位】")
        .requiredRow(RowPosition.FRONT)
        .attackMultiplier(1.2)
        .specialBonusDesc("威力+20%")
        .build();

    FormationSlot slot1 = FormationSlot.builder()
        .slotIndex(1)
        .slotName("玉衡【青龍生息】")
        .requiredRow(RowPosition.BACK)
        .defenseMultiplier(1.3)
        .specialBonusDesc("免傷+30%")
        .build();

    FormationTemplate formation = FormationTemplate.builder()
        .id("test_formation")
        .name("八卦大陣")
        .slots(List.of(slot0, slot1))
        .build();

    PartyMember warrior = PartyMember.builder()
        .id("warrior")
        .name("鐵牛")
        .row(RowPosition.FRONT)
        .baseMinDamage(10)
        .baseMaxDamage(20)
        .baseDefense(10)
        .build();

    PartyMember cleric = PartyMember.builder()
        .id("cleric")
        .name("凌霜")
        .row(RowPosition.BACK)
        .baseMinDamage(5)
        .baseMaxDamage(10)
        .baseDefense(5)
        .build();

    Party party = Party.builder()
        .id("p-form")
        .equippedFormation(formation)
        .members(new ArrayList<>(List.of(warrior, cleric)))
        .build();

    // 初始：鐵牛在 Slot 0 (FRONT)，站位符合 FRONT，享受 1.2 倍攻擊
    var stats0 = party.calculateEffectiveStats(0);
    assertEquals(24, stats0.maxDamage()); // 20 * 1.2 = 24

    // 換位：將鐵牛調至 Slot 1 (要求 BACK)，凌霜調至 Slot 0 (要求 FRONT)
    party.swapMembers(0, 1);

    // 鐵牛現在在 Slot 1，但其站位仍是 FRONT，與 Slot 1 要求的 BACK 不符！
    // 依業務規則：站位不符不享有孔位倍率加成
    var slotForIron = party.getEquippedFormation().getSlot(1);
    boolean match = (slotForIron.getRequiredRow() == RowPosition.ANY || slotForIron.getRequiredRow() == party.getMembers().get(1).getRow());
    assertFalse(match, "鐵牛前衛站位不應匹配 Slot 1 的後衛要求");
  }
}
