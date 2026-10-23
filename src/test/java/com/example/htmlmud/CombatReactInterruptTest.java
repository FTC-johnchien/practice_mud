package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy.CastingState;
import com.example.htmlmud.domain.dungeon.battle.CombatFsmService;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.RowPosition;

/**
 * Milestone F2-01: 敵方行動預告蓄力條與即時反制打斷 (Combat React, COMBAT-REACT-01) 專屬整合測試
 */
class CombatReactInterruptTest {

  private CombatFsmService fsmService;
  private PartyMember swordsman;
  private PartyMember monk;

  @BeforeEach
  void setUp() {
    fsmService = new CombatFsmService();
    swordsman = PartyMember.builder()
        .id("swordsman-01")
        .name("白衣劍客")
        .build();
    monk = PartyMember.builder()
        .id("monk-01")
        .name("怒目金剛")
        .build();
  }

  @Test
  @DisplayName("F2-01-T1: 驗證怪物進入施法蓄力狀態與 CastingState 模型流轉")
  void testEnemyEntersCastingStateAndProgressTracks() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-black-goat")
        .name("黑山羊古神眷族·畸變祭司")
        .hp(800)
        .maxHp(800)
        .poise(100)
        .maxPoise(100)
        .row(RowPosition.BACK)
        .build();

    assertThat(boss.getCastingState()).isEqualTo(CastingState.IDLE);
    assertThat(boss.isCasting()).isFalse();

    // 啟動 3000ms 的強力外神儀式蓄力
    fsmService.startEnemyCast(boss, "👁️", "【莎布血霧獻祭】", "SPELL", "ALL", 3000L, true, "aberration_sacrifice");

    assertThat(boss.isCasting()).isTrue();
    assertThat(boss.getCastingState()).isEqualTo(CastingState.CASTING);
    assertThat(boss.getCurrentCastingSkillId()).isEqualTo("aberration_sacrifice");
    assertThat(boss.getCurrentAction()).isEqualTo("【莎布血霧獻祭】");
    assertThat(boss.isInterruptible()).isTrue();

    // 推進時間：剩餘 1500ms -> 進度 50%
    boss.setCastRemainingMs(1500L);
    assertThat(boss.getActionProgressPercent()).isEqualTo(50);

    // 完成蓄力
    boss.finishCast();
    assertThat(boss.getCastingState()).isEqualTo(CastingState.COMPLETED);
    assertThat(boss.isCasting()).isFalse();
    assertThat(boss.getCurrentIntentName()).isNull();
  }

  @Test
  @DisplayName("F2-01-T2: 驗證反制技能 (STUN / SILENCE / INTERRUPT) 成功打斷敵方蓄力並引發失衡力竭")
  void testCounterSkillsInterruptEnemyCasting() {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("shrine-guardian")
        .name("賽博殖民地·生化禁衛")
        .hp(400)
        .maxHp(400)
        .poise(60)
        .maxPoise(60)
        .row(RowPosition.FRONT)
        .build();

    // 怪物進入可打斷蓄力
    fsmService.startEnemyCast(enemy, "⚡", "【高能粒子炮過載】", "HEAVY", "SINGLE", 2500L, true, "laser_overload");

    // 玩家施展反制招式【太白斷空】(帶有 INTERRUPT 與 SILENCE 標籤)
    PartyMemberSkill interruptSkill = PartyMemberSkill.builder()
        .id("taibai_interrupt")
        .name("太白斷空")
        .damageMultiplier(1.6)
        .tags(List.of("ATTACK", "SWORD", "INTERRUPT", "SILENCE"))
        .stun(false)
        .build();

    assertThat(fsmService.canInterrupt(interruptSkill)).isTrue();

    var result = fsmService.resolveSkillInterrupt(enemy, swordsman, interruptSkill);

    assertThat(result.interrupted()).isTrue();
    assertThat(result.interruptedAction()).isEqualTo("【高能粒子炮過載】");
    assertThat(result.combatLog()).contains("⚡【反制打斷!】白衣劍客 施展【太白斷空】擊破【賽博殖民地·生化禁衛】");

    // 驗證敵人狀態：蓄力被中斷，進入 INTERRUPTED 狀態，罡氣破防失衡
    assertThat(enemy.isCasting()).isFalse();
    assertThat(enemy.getCastingState()).isEqualTo(CastingState.INTERRUPTED);
    assertThat(enemy.isPoiseBroken()).isTrue();
    assertThat(enemy.isStunned()).isTrue();
  }

  @Test
  @DisplayName("F2-01-T3: 驗證罡氣破防打斷 (Poise Break Interrupt) 機制")
  void testPoiseBreakInterruptsEnemyCasting() {
    BattleEnemy elite = BattleEnemy.builder()
        .id("elite-cultist")
        .name("黑煞宗·築基期執事")
        .hp(600)
        .maxHp(600)
        .poise(40)
        .maxPoise(40)
        .row(RowPosition.FRONT)
        .build();

    // 執事正在吟唱黑煞血煞咒
    fsmService.startEnemyCast(elite, "🔥", "【黑煞噬魂咒】", "SPELL", "SINGLE", 2000L, true);
    assertThat(elite.isCasting()).isTrue();

    // 假設一名僧人施展重拳打擊，造成 45 點 Poise 傷害打破了敵人的護體罡氣
    elite.applyPoiseDamage(45);
    assertThat(elite.isPoiseBroken()).isTrue();

    // 判定罡氣破防打斷
    var poiseResult = fsmService.resolvePoiseBreakInterrupt(elite, monk);

    assertThat(poiseResult.interrupted()).isTrue();
    assertThat(poiseResult.combatLog()).contains("⚡【罡氣破防打斷!】怒目金剛 猛烈攻勢震散【黑煞宗·築基期執事】護體罡氣！");
    assertThat(elite.isCasting()).isFalse();
    assertThat(elite.getCastingState()).isEqualTo(CastingState.INTERRUPTED);
  }

  @Test
  @DisplayName("F2-01-T4: 驗證霸體 (Uninterruptible) 怪物免疫反制打斷")
  void testUninterruptibleEnemyImmuneToInterrupt() {
    BattleEnemy tyrant = BattleEnemy.builder()
        .id("boss-tyrant")
        .name("母神卵鞘·暴走降臨者")
        .hp(1500)
        .maxHp(1500)
        .poise(300)
        .maxPoise(300)
        .row(RowPosition.FRONT)
        .build();

    // 啟動不可打斷的狂暴霸體蓄力
    fsmService.startEnemyCast(tyrant, "💥", "【母神胎動天崩地裂】", "HEAVY", "ALL", 3500L, false);

    assertThat(tyrant.isCasting()).isTrue();
    assertThat(tyrant.isInterruptible()).isFalse();

    PartyMemberSkill stunSkill = PartyMemberSkill.builder()
        .id("tank_smash")
        .name("裂地崩山")
        .stun(true)
        .stunDurationSeconds(3)
        .tags(List.of("STUN", "CONTROL"))
        .build();

    var result = fsmService.resolveSkillInterrupt(tyrant, monk, stunSkill);

    // 霸體怪物免疫打斷
    assertThat(result.interrupted()).isFalse();
    assertThat(tyrant.isCasting()).isTrue();
    assertThat(tyrant.getCastingState()).isEqualTo(CastingState.CASTING);
    assertThat(tyrant.getCurrentAction()).isEqualTo("【母神胎動天崩地裂】");
  }

  @Test
  @DisplayName("F2-01-T5: 驗證 BattleEnemyViewDto 正確投射蓄力進度條與可打斷屬性")
  void testBattleEnemyViewDtoProjection() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-corrupted")
        .name("千星科技·叛逃生化督導")
        .hp(700)
        .maxHp(700)
        .row(RowPosition.FRONT)
        .build();

    fsmService.startEnemyCast(boss, "🧪", "【青玉髓超頻注射】", "BUFF", "SELF", 2000L, true);
    boss.setCastRemainingMs(500L); // 剩餘 500ms -> 進度 75%

    BattleEnemyViewDto dto = new BattleEnemyViewDto(
        0, boss.getId(), boss.getName(), boss.getHp(), boss.getMaxHp(),
        boss.getRow().name(), true, false, false,
        "member-1", "白衣劍客", 50, false, 1, "FRONT", 0, 0, 1, 1,
        boss.getCurrentIntentIcon(), boss.getCurrentIntentName(),
        boss.getCurrentIntentType(), boss.getIntentTargetScope(),
        boss.isCasting(), boss.getCastDurationMs(), boss.getCastRemainingMs(),
        boss.isInterruptible()
    );

    assertThat(dto.isCasting()).isTrue();
    assertThat(dto.currentAction()).isEqualTo("【青玉髓超頻注射】");
    assertThat(dto.actionProgressPercent()).isEqualTo(75);
    assertThat(dto.interruptible()).isTrue();
  }
}
