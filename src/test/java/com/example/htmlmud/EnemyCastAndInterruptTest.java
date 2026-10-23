package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.combat.BattleEnemy;
import com.example.htmlmud.combat.CombatFsmService;
import com.example.htmlmud.combat.BattleEnemyDto;
import com.example.htmlmud.combat.BattleEnemyViewDto;
import com.example.htmlmud.combat.CombatMobDto;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.RowPosition;

/**
 * 敵方行動預告蓄力條與打斷反制機制之專屬機制測試 (Milestone 1 - Task 3)
 */
class EnemyCastAndInterruptTest {

  private CombatFsmService fsmService;
  private PartyMember warrior;
  private PartyMember taoist;

  @BeforeEach
  void setUp() {
    fsmService = new CombatFsmService();
    warrior = PartyMember.builder()
        .id("member-warrior")
        .name("凌雲劍客")
        .build();
    taoist = PartyMember.builder()
        .id("member-taoist")
        .name("清微道長")
        .build();
  }

  @Test
  @DisplayName("CAST-01: 測試蓄力進度隨時間動態推移與百分比精確度")
  void testCastProgressAndDecayCalculation() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-song")
        .name("畸變大師兄·宋天衡")
        .hp(500)
        .maxHp(500)
        .row(RowPosition.FRONT)
        .build();

    // 啟動 2000ms 的可打斷法術蓄力
    fsmService.startEnemyCast(boss, "⚡", "【萬雷轟頂】", "SPELL", "ALL", 2000L, true);

    assertThat(boss.isCasting()).isTrue();
    assertThat(boss.getCastDurationMs()).isEqualTo(2000L);
    assertThat(boss.getCastRemainingMs()).isGreaterThan(0).isLessThanOrEqualTo(2000L);
    assertThat(boss.getCurrentAction()).isEqualTo("【萬雷轟頂】");
    assertThat(boss.isInterruptible()).isTrue();

    // 模擬剩餘時間推移：剩餘 1000ms -> 進度應為 50%
    boss.setCastRemainingMs(1000L);
    assertThat(boss.getActionProgressPercent()).isEqualTo(50);

    // 模擬剩餘 200ms -> 進度應為 90%
    boss.setCastRemainingMs(200L);
    assertThat(boss.getActionProgressPercent()).isEqualTo(90);

    // 模擬剩餘 0ms -> 進度應為 100%
    boss.setCastRemainingMs(0L);
    assertThat(boss.getActionProgressPercent()).isEqualTo(100);

    // 轉為 DTO 視圖驗證進度欄位傳遞一致
    BattleEnemyViewDto viewDto = new BattleEnemyViewDto(
        0, boss.getId(), boss.getName(), boss.getHp(), boss.getMaxHp(),
        boss.getRow().name(), true, boss.isPoiseBroken(), boss.isStunned(),
        "member-warrior", "凌雲劍客", 100, false, 1, "CENTER", 1, 2, 1, 1,
        boss.getCurrentIntentIcon(), boss.getCurrentIntentName(),
        boss.getCurrentIntentType(), boss.getIntentTargetScope(),
        boss.isCasting(), boss.getCastDurationMs(), boss.getCastRemainingMs(),
        boss.isInterruptible()
    );

    assertThat(viewDto.currentAction()).isEqualTo("【萬雷轟頂】");
    assertThat(viewDto.actionProgressPercent()).isEqualTo(100);
    assertThat(viewDto.interruptible()).isTrue();
  }

  @Test
  @DisplayName("CAST-02: 測試控場標籤反制矩陣 (STUN, SILENCE, CONTROL, SEAL)")
  void testTagCounterMatrixInterrupts() {
    List<String> validTags = List.of("STUN", "SILENCE", "CONTROL", "SEAL");

    for (String tag : validTags) {
      BattleEnemy enemy = BattleEnemy.builder()
          .id("mob-" + tag)
          .name("試煉傀儡")
          .hp(200)
          .maxHp(200)
          .poise(50)
          .maxPoise(50)
          .build();

      fsmService.startEnemyCast(enemy, "🔥", "煉獄火雨", "SPELL", "ALL", 1500L, true);
      assertThat(enemy.isCasting()).isTrue();

      PartyMemberSkill skillWithTag = PartyMemberSkill.builder()
          .id("skill_" + tag.toLowerCase())
          .name("破招技·" + tag)
          .tags(List.of("ATTACK", tag))
          .stun(true)
          .stunDurationSeconds(2)
          .build();

      var result = fsmService.resolveSkillInterrupt(enemy, taoist, skillWithTag);

      assertThat(result.interrupted())
          .as("具備標籤 " + tag + " 應能成功打斷可打斷招式")
          .isTrue();
      assertThat(result.interruptedAction()).isEqualTo("煉獄火雨");
      assertThat(result.combatLog()).contains("破招技·" + tag);
      assertThat(enemy.isCasting()).isFalse();
      assertThat(enemy.isPoiseBroken()).isTrue();
      assertThat(enemy.isStunned()).isTrue();
    }
  }

  @Test
  @DisplayName("CAST-03: 測試首領招式霸體 (interruptible=false) 免疫各類反制")
  void testUninterruptibleBossCastImmunity() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-iron")
        .name("黑煞魔將")
        .hp(800)
        .maxHp(800)
        .poise(100)
        .maxPoise(100)
        .build();

    // 啟動霸體狂暴招式 (interruptible = false)
    fsmService.startEnemyCast(boss, "💀", "【修羅滅絕斬】", "HEAVY", "FRONT", 3000L, false);

    assertThat(boss.isCasting()).isTrue();
    assertThat(boss.isInterruptible()).isFalse();
    assertThat(boss.getInterruptible()).isFalse();

    // 嘗試使用最強大的控制技能進行打斷
    PartyMemberSkill superStunSkill = PartyMemberSkill.builder()
        .id("skill_ultimate_seal")
        .name("九天玄女禁神符")
        .tags(List.of("SPELL", "CONTROL", "STUN", "SILENCE", "SEAL"))
        .stun(true)
        .stunDurationSeconds(5)
        .build();

    var result = fsmService.resolveSkillInterrupt(boss, taoist, superStunSkill);

    // 霸體招式不可被打斷
    assertThat(result.interrupted()).isFalse();
    assertThat(result.combatLog()).isNull();
    assertThat(boss.isCasting()).isTrue();
    assertThat(boss.getCurrentAction()).isEqualTo("【修羅滅絕斬】");
    assertThat(boss.isPoiseBroken()).isFalse();
  }

  @Test
  @DisplayName("CAST-04: 測試無打斷標籤之普通技能無法觸發反制打斷")
  void testNonInterruptingSkillsFailToInterrupt() {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-wolf")
        .name("幽冥魔狼")
        .hp(150)
        .maxHp(150)
        .build();

    fsmService.startEnemyCast(enemy, "🐺", "魔狼撕咬", "PHYSICAL", "SINGLE", 1200L, true);

    // 純傷害物理技能
    PartyMemberSkill slashSkill = PartyMemberSkill.builder()
        .id("slash")
        .name("清風十三劍")
        .tags(List.of("ATTACK", "PHYSICAL", "SLASH"))
        .build();

    var slashResult = fsmService.resolveSkillInterrupt(enemy, warrior, slashSkill);
    assertThat(slashResult.interrupted()).isFalse();
    assertThat(enemy.isCasting()).isTrue();

    // 純火系法術傷害技能
    PartyMemberSkill fireSkill = PartyMemberSkill.builder()
        .id("fire_blast")
        .name("離火烈焰")
        .tags(List.of("SPELL", "FIRE", "DAMAGE"))
        .build();

    var fireResult = fsmService.resolveSkillInterrupt(enemy, taoist, fireSkill);
    assertThat(fireResult.interrupted()).isFalse();
    assertThat(enemy.isCasting()).isTrue();
  }

  @Test
  @DisplayName("CAST-05: 測試怪物未處於蓄力施法階段時不觸發打斷判定")
  void testNoInterruptWhenEnemyNotCasting() {
    BattleEnemy idleEnemy = BattleEnemy.builder()
        .id("mob-idle")
        .name("幽靈守衛")
        .hp(100)
        .maxHp(100)
        .build();

    assertThat(idleEnemy.isCasting()).isFalse();

    PartyMemberSkill stunSkill = PartyMemberSkill.builder()
        .id("stun_smash")
        .name("撼地重擊")
        .tags(List.of("ATTACK", "STUN", "CONTROL"))
        .stun(true)
        .stunDurationSeconds(3)
        .build();

    var result = fsmService.resolveSkillInterrupt(idleEnemy, warrior, stunSkill);
    assertThat(result.interrupted()).isFalse();
    assertThat(result.combatLog()).isNull();
  }

  @Test
  @DisplayName("CAST-06: 測試打斷觸發破防失衡、清空意圖與攻擊間隔推遲")
  void testPoiseBreakAndAttackDelayOnInterrupt() {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-guard")
        .name("太陰古衛")
        .hp(200)
        .maxHp(200)
        .poise(40)
        .maxPoise(40)
        .build();

    fsmService.startEnemyCast(enemy, "🛡️", "玄陰重盾衝擊", "HEAVY", "FRONT", 1800L, true);

    long now = System.currentTimeMillis();
    enemy.setNextAttackTime(now + 500); // 原本 500ms 後發動攻擊

    PartyMemberSkill sealSkill = PartyMemberSkill.builder()
        .id("seal_strike")
        .name("定身咒")
        .tags(List.of("CONTROL"))
        .stun(true)
        .stunDurationSeconds(2)
        .build();

    var result = fsmService.resolveSkillInterrupt(enemy, taoist, sealSkill);

    assertThat(result.interrupted()).isTrue();
    // 驗證蓄力與意圖清空
    assertThat(enemy.isCasting()).isFalse();
    assertThat(enemy.getCurrentIntentName()).isNull();
    assertThat(enemy.getCurrentAction()).isEmpty();

    // 驗證破防失衡狀態觸發
    assertThat(enemy.isPoiseBroken()).isTrue();
    assertThat(enemy.getPoise()).isEqualTo(0);

    // 驗證行動延遲（被打斷後 nextAttackTime 應大幅推遲，給予小隊輸出窗口）
    assertThat(enemy.getNextAttackTime()).isGreaterThan(now + 1000);

    // 驗證 popLastInterruptReason 具有一次性消費特性
    assertThat(enemy.popLastInterruptReason()).contains("定身咒").contains("反制打斷");
    assertThat(enemy.popLastInterruptReason()).isNull();
  }

  @Test
  @DisplayName("CAST-07: 測試群體 AOE 技能對多個蓄力與非蓄力目標的獨立解算")
  void testAoeInterruptResolutionOnMultipleEnemies() {
    // 目標 A: 蓄力可打斷技能
    BattleEnemy mobA = BattleEnemy.builder()
        .id("mob-a")
        .name("陰風巫祝")
        .hp(120)
        .maxHp(120)
        .poise(30)
        .maxPoise(30)
        .build();
    fsmService.startEnemyCast(mobA, "🔮", "引魂咒", "SPELL", "SINGLE", 2000L, true);

    // 目標 B: 蓄力霸體不可打斷技能
    BattleEnemy mobB = BattleEnemy.builder()
        .id("mob-b")
        .name("暴怒血煞")
        .hp(300)
        .maxHp(300)
        .poise(80)
        .maxPoise(80)
        .build();
    fsmService.startEnemyCast(mobB, "⚔️", "瘋狂旋風斬", "HEAVY", "ALL", 2500L, false);

    // 目標 C: 普通待機狀態
    BattleEnemy mobC = BattleEnemy.builder()
        .id("mob-c")
        .name("普通小妖")
        .hp(80)
        .maxHp(80)
        .poise(20)
        .maxPoise(20)
        .build();

    PartyMemberSkill aoeSealSkill = PartyMemberSkill.builder()
        .id("combo_four_symbols_seal")
        .name("四象封魔印")
        .aoe(true)
        .tags(List.of("SPELL", "CONTROL", "SEAL"))
        .stun(true)
        .stunDurationSeconds(3)
        .build();

    // 模擬 AOE 遍歷三隻怪
    var resA = fsmService.resolveSkillInterrupt(mobA, taoist, aoeSealSkill);
    var resB = fsmService.resolveSkillInterrupt(mobB, taoist, aoeSealSkill);
    var resC = fsmService.resolveSkillInterrupt(mobC, taoist, aoeSealSkill);

    // 驗證各目標結果完全獨立且精確符合規範
    assertThat(resA.interrupted()).isTrue();
    assertThat(mobA.isCasting()).isFalse();
    assertThat(mobA.isPoiseBroken()).isTrue();

    assertThat(resB.interrupted()).isFalse();
    assertThat(mobB.isCasting()).isTrue();
    assertThat(mobB.isPoiseBroken()).isFalse();

    assertThat(resC.interrupted()).isFalse();
    assertThat(mobC.isCasting()).isFalse();
  }

  @Test
  @DisplayName("CAST-08: 測試 DTO 契約一致性 (BattleEnemyDto, CombatMobDto, BattleEnemyViewDto)")
  void testDtoContractIntegrity() {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-test")
        .name("怨魂守衛")
        .hp(180)
        .maxHp(200)
        .castDurationMs(1600L)
        .interruptible(true)
        .build();

    fsmService.startEnemyCast(enemy, "💀", "厲鬼索命", "SPELL", "SINGLE", 1600L, true);
    enemy.setCastRemainingMs(800L); // 進度 50%

    // 測試 BattleEnemyDto
    BattleEnemyDto enemyDto = BattleEnemyDto.from(2, enemy);
    assertThat(enemyDto.index()).isEqualTo(2);
    assertThat(enemyDto.name()).isEqualTo("怨魂守衛");
    assertThat(enemyDto.currentAction()).isEqualTo("厲鬼索命");
    assertThat(enemyDto.actionProgressPercent()).isCloseTo(50, org.assertj.core.data.Offset.offset(2));
    assertThat(enemyDto.interruptible()).isTrue();

    // 測試 CombatMobDto
    CombatMobDto mobDto = CombatMobDto.of(
        enemy.getId(),
        enemy.getName(),
        enemy.getHp(),
        enemy.getMaxHp(),
        enemy.getCurrentAction(),
        enemy.getActionProgressPercent(),
        enemy.isInterruptible()
    );
    assertThat(mobDto.id()).isEqualTo("mob-test");
    assertThat(mobDto.currentAction()).isEqualTo("厲鬼索命");
    assertThat(mobDto.actionProgressPercent()).isCloseTo(50, org.assertj.core.data.Offset.offset(2));
    assertThat(mobDto.interruptible()).isTrue();
  }
}
