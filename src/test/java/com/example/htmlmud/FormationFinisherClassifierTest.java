package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.combat.BattleContext;
import com.example.htmlmud.combat.BattleEnemy;
import com.example.htmlmud.combat.BattleEvent;
import com.example.htmlmud.combat.ComboResolver;
import com.example.htmlmud.combat.FormationFinisherClassifier;
import com.example.htmlmud.combat.FormationFinisherSpec;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.FormationTemplate;

class FormationFinisherClassifierTest {

  private Party createPartyWithSchools(String leaderSchool, String... memberSchools) {
    List<PartyMember> members = new ArrayList<>();
    PartyMember leader = PartyMember.builder()
        .id("m-leader")
        .name("隊長")
        .originSchool(leaderSchool)
        .build();
    members.add(leader);

    int idx = 1;
    for (String s : memberSchools) {
      members.add(PartyMember.builder()
          .id("m-" + idx)
          .name("隊員" + idx)
          .originSchool(s)
          .build());
      idx++;
    }

    FormationTemplate formation = FormationTemplate.builder()
        .id("formation_five_elements")
        .name("五行大陣")
        .requiredPartySize(5)
        .build();

    return Party.builder()
        .id("test-party")
        .partyName("測試陣法隊伍")
        .members(members)
        .equippedFormation(formation)
        .formationEnergy(100)
        .build();
  }

  @Test
  @DisplayName("REFACTOR-P1-01: 驗證四大純粹極限型小隊終結技判定")
  void testFourPureFinishers() {
    // 1. 純修仙者隊 (UNIVERSAL 隊長 + 4 修仙)
    Party pureCultivator = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "CULTIVATOR", "CULTIVATOR", "CULTIVATOR");
    FormationFinisherSpec spec1 = FormationFinisherClassifier.classify(pureCultivator);
    assertThat(spec1.id()).isEqualTo("finisher_pure_cultivator");
    assertThat(spec1.name()).isEqualTo("【萬劍歸宗·九天太乙誅仙劍陣】");
    assertThat(spec1.damageType()).isEqualTo("HOLY");

    // 2. 純古代工程師隊 (UNIVERSAL 隊長 + 4 工程師)
    Party pureEngineer = createPartyWithSchools("UNIVERSAL", "ENGINEER", "ENGINEER", "ENGINEER", "ENGINEER");
    FormationFinisherSpec spec2 = FormationFinisherClassifier.classify(pureEngineer);
    assertThat(spec2.id()).isEqualTo("finisher_pure_engineer");
    assertThat(spec2.name()).isEqualTo("【大地沉淪·太古神山崩天闕】");
    assertThat(spec2.damageType()).isEqualTo("EARTH");

    // 3. 純高科技隊 (UNIVERSAL 隊長 + 4 高科技)
    Party pureHighTech = createPartyWithSchools("UNIVERSAL", "HIGH_TECH", "HIGH_TECH", "HIGH_TECH", "HIGH_TECH");
    FormationFinisherSpec spec3 = FormationFinisherClassifier.classify(pureHighTech);
    assertThat(spec3.id()).isEqualTo("finisher_pure_high_tech");
    assertThat(spec3.name()).isEqualTo("【天衍極限·千星神念天網劫】");
    assertThat(spec3.damageType()).isEqualTo("LIGHTNING");

    // 4. 純克蘇魯隊 (UNIVERSAL 隊長 + 4 克蘇魯)
    Party pureCthulhu = createPartyWithSchools("UNIVERSAL", "CTHULHU", "CTHULHU", "CTHULHU", "CTHULHU");
    FormationFinisherSpec spec4 = FormationFinisherClassifier.classify(pureCthulhu);
    assertThat(spec4.id()).isEqualTo("finisher_pure_cthulhu");
    assertThat(spec4.name()).isEqualTo("【森羅萬象·黑山羊母神血宴】");
    assertThat(spec4.damageType()).isEqualTo("DARK");
  }

  @Test
  @DisplayName("REFACTOR-P1-02: 驗證萬象混元型小隊終結技判定 (四大道統均涵蓋)")
  void testUniversalFinisher() {
    Party universalParty = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "ENGINEER", "HIGH_TECH", "CTHULHU");
    FormationFinisherSpec spec = FormationFinisherClassifier.classify(universalParty);
    assertThat(spec.id()).isEqualTo("finisher_all_universal");
    assertThat(spec.name()).isEqualTo("【大衍混元·諸天萬象開天闢地神劫】");
    assertThat(spec.damageType()).isEqualTo("CHAOS");
    assertThat(spec.damageMultiplierBonus()).isEqualTo(1.0);
    assertThat(spec.poiseBonus()).isEqualTo(60);
  }

  @Test
  @DisplayName("REFACTOR-P1-03: 驗證六大雙曜交織型小隊終結技判定")
  void testSixDualFinishers() {
    // 1. 修仙 + 克蘇魯 (仙魔雙曜)
    Party xianMo = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "CULTIVATOR", "CTHULHU", "CTHULHU");
    assertThat(FormationFinisherClassifier.classify(xianMo).id()).isEqualTo("finisher_xian_mo");
    assertThat(FormationFinisherClassifier.classify(xianMo).name()).isEqualTo("【太極血海·逆亂生死神魔印】");

    // 2. 高科技 + 工程師 (賽博神機)
    Party cyberEng = createPartyWithSchools("UNIVERSAL", "HIGH_TECH", "HIGH_TECH", "ENGINEER", "ENGINEER");
    assertThat(FormationFinisherClassifier.classify(cyberEng).id()).isEqualTo("finisher_cyber_engineer");
    assertThat(FormationFinisherClassifier.classify(cyberEng).name()).isEqualTo("【天基軌道·萬鈞乾坤神機炮】");

    // 3. 修仙 + 高科技 (仙道賽博)
    Party xianCyber = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "CULTIVATOR", "HIGH_TECH", "HIGH_TECH");
    assertThat(FormationFinisherClassifier.classify(xianCyber).id()).isEqualTo("finisher_xian_cyber");
    assertThat(FormationFinisherClassifier.classify(xianCyber).name()).isEqualTo("【九天星網·神念超頻萬劍矩陣】");

    // 4. 修仙 + 工程師 (天工仙道)
    Party xianEng = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "CULTIVATOR", "ENGINEER", "ENGINEER");
    assertThat(FormationFinisherClassifier.classify(xianEng).id()).isEqualTo("finisher_xian_engineer");
    assertThat(FormationFinisherClassifier.classify(xianEng).name()).isEqualTo("【天工造化·九嶽神山崩天闕】");

    // 5. 克蘇魯 + 高科技 (生化黑客)
    Party cthulhuCyber = createPartyWithSchools("UNIVERSAL", "CTHULHU", "CTHULHU", "HIGH_TECH", "HIGH_TECH");
    assertThat(FormationFinisherClassifier.classify(cthulhuCyber).id()).isEqualTo("finisher_cthulhu_cyber");
    assertThat(FormationFinisherClassifier.classify(cthulhuCyber).name()).isEqualTo("【生化天羅·黑煞奈米噬魂木馬】");

    // 6. 克蘇魯 + 工程師 (血肉泰坦)
    Party cthulhuEng = createPartyWithSchools("UNIVERSAL", "CTHULHU", "CTHULHU", "ENGINEER", "ENGINEER");
    assertThat(FormationFinisherClassifier.classify(cthulhuEng).id()).isEqualTo("finisher_cthulhu_engineer");
    assertThat(FormationFinisherClassifier.classify(cthulhuEng).name()).isEqualTo("【深淵巨擘·太古血肉魔導重機】");
  }

  @Test
  @DisplayName("REFACTOR-P1-04: 驗證四大三才鼎立型小隊終結技判定")
  void testFourTrioFinishers() {
    // 1. 修仙 + 克蘇魯 + 高科技 (排除工程師)
    Party trio1 = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "CULTIVATOR", "CTHULHU", "HIGH_TECH");
    assertThat(FormationFinisherClassifier.classify(trio1).id()).isEqualTo("finisher_trio_cultivator_cthulhu_cyber");
    assertThat(FormationFinisherClassifier.classify(trio1).name()).isEqualTo("【偷天換日·生化天道逆亂真訣】");

    // 2. 修仙 + 克蘇魯 + 工程師 (排除高科技)
    Party trio2 = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "CTHULHU", "ENGINEER", "ENGINEER");
    assertThat(FormationFinisherClassifier.classify(trio2).id()).isEqualTo("finisher_trio_cultivator_cthulhu_engineer");
    assertThat(FormationFinisherClassifier.classify(trio2).name()).isEqualTo("【冥河重嶽·混沌太虛開山印】");

    // 3. 修仙 + 高科技 + 工程師 (排除外神)
    Party trio3 = createPartyWithSchools("UNIVERSAL", "CULTIVATOR", "HIGH_TECH", "ENGINEER", "ENGINEER");
    assertThat(FormationFinisherClassifier.classify(trio3).id()).isEqualTo("finisher_trio_cultivator_cyber_engineer");
    assertThat(FormationFinisherClassifier.classify(trio3).name()).isEqualTo("【天衍算籌·九天神雷星際裂變陣】");

    // 4. 克蘇魯 + 高科技 + 工程師 (排除修仙)
    Party trio4 = createPartyWithSchools("UNIVERSAL", "CTHULHU", "HIGH_TECH", "ENGINEER", "ENGINEER");
    assertThat(FormationFinisherClassifier.classify(trio4).id()).isEqualTo("finisher_trio_cthulhu_cyber_engineer");
    assertThat(FormationFinisherClassifier.classify(trio4).name()).isEqualTo("【萬機歸淵·黑山羊奇異點格式化】");
  }

  @Test
  @DisplayName("REFACTOR-P1-05: 驗證 ComboResolver 執行天外合擊時正確調用動態終結技並產生對應事件")
  void testComboResolverExecutionWithDynamicFinisher() {
    ComboResolver resolver = new ComboResolver();
    Party party = createPartyWithSchools("UNIVERSAL", "HIGH_TECH", "HIGH_TECH", "ENGINEER", "ENGINEER");

    BattleContext ctx = new BattleContext();
    ctx.setParty(party);
    ctx.setResonancePoints(3); // 達標可施放

    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-1")
        .name("墨竹異化巨蠍")
        .hp(1000)
        .maxHp(1000)
        .poise(100)
        .maxPoise(100)
        .alive(true)
        .build();
    ctx.getEnemies().add(enemy);

    PartyMember invoker = party.getMembers().get(0);
    List<String> logs = new ArrayList<>();

    boolean success = resolver.executeFormationFinisher(null, ctx, invoker, (p, msg) -> logs.add(msg));

    assertThat(success).isTrue();
    assertThat(ctx.getResonancePoints()).isEqualTo(0);
    assertThat(enemy.getHp()).isLessThan(1000);

    // 驗證戰鬥日誌包含對應之終結技名稱
    String combinedLog = String.join("\n", logs);
    assertThat(combinedLog).contains("【天基軌道·萬鈞乾坤神機炮】");
    assertThat(combinedLog).contains("天基鎢棒");

    // 驗證發派事件之技能 ID 與名稱
    List<BattleEvent> events = new ArrayList<>(ctx.getPendingEvents());
    assertThat(events).isNotEmpty();
    var lastEvent = events.get(events.size() - 1);
    assertThat(lastEvent.skillId()).isEqualTo("finisher_cyber_engineer");
    assertThat(lastEvent.skillName()).isEqualTo("【天基軌道·萬鈞乾坤神機炮】");
    assertThat(lastEvent.fxKey()).isEqualTo("formation_finisher");
  }
}
