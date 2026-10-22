package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.combat.CombatRules;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyDto;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.BuffType;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.repository.TemplateReader;

/**
 * Milestone F2-02: 罡氣破防重創與首領多階段狂暴轉階段 (Poise Break & Boss Phases) 專屬整合測試
 */
@SpringBootTest
class BossPhaseAndPoiseStaggerTest {

  @Autowired
  private TemplateReader templateReader;

  @Test
  @DisplayName("F2-02-T1: 驗證罡氣破防 (Poise Broken) 1.5倍致命暴擊傷害公式與力竭狀態")
  void testPoiseBreakStaggerCritFormula() {
    int normalDmg = CombatRules.applyPoiseBreakCrit(100, false);
    assertThat(normalDmg).isEqualTo(100);

    int staggerCritDmg = CombatRules.applyPoiseBreakCrit(100, true);
    assertThat(staggerCritDmg).isEqualTo(150);

    BattleEnemy enemy = BattleEnemy.builder()
        .id("test-enemy")
        .name("測試傀儡")
        .hp(200)
        .maxHp(200)
        .poise(50)
        .maxPoise(50)
        .build();

    assertThat(enemy.isPoiseBroken()).isFalse();

    // 削韌至歸零觸發破防
    enemy.applyPoiseDamage(50);
    assertThat(enemy.isPoiseBroken()).isTrue();
    assertThat(enemy.getPoiseBrokenTicksRemaining()).isEqualTo(14); // 7 秒力竭期
  }

  @Test
  @DisplayName("F2-02-T2: 驗證首領多階段 (Boss Phases) 閾值求值與狂暴過載狀態機流轉")
  void testBossPhaseModelAndThresholds() {
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-test")
        .name("畸變領主")
        .rank(MobRank.BOSS)
        .hp(1000)
        .maxHp(1000)
        .currentPhase(1)
        .maxPhases(3)
        .phaseThresholds(List.of(70, 30))
        .attackIntervalMs(2000)
        .isEnraged(false)
        .build();

    // 氣血 80% (800 HP) -> 未達 70% 閾值
    boss.setHp(800);
    assertThat(boss.checkPhaseTransition()).isEqualTo(-1);

    // 氣血跌破 70% (700 HP) -> 觸發 Phase 2
    boss.setHp(700);
    assertThat(boss.checkPhaseTransition()).isEqualTo(2);

    boss.transitionToPhase(2);
    assertThat(boss.getCurrentPhase()).isEqualTo(2);
    assertThat(boss.isEnraged()).isFalse();

    // 氣血跌破 30% (250 HP) -> 觸發 Phase 3 (最終階段，狂暴超載)
    boss.setHp(250);
    assertThat(boss.checkPhaseTransition()).isEqualTo(3);

    boss.transitionToPhase(3);
    assertThat(boss.getCurrentPhase()).isEqualTo(3);
    assertThat(boss.isEnraged()).isTrue();

    // 狂暴攻速加成：基礎間隔 2000ms，在狂暴 (+30% 攻速) 下應有效縮短
    long interval = boss.getEffectiveAttackIntervalMs();
    assertThat(interval).isLessThan(2000L);
    assertThat(interval).isEqualTo(1400L);
  }

  @Test
  @DisplayName("F2-02-T3: 驗證轉階段協議：淨化負面 Debuff、解除眩暈、重聚罡氣且上限攀升 25%")
  void testPhaseTransitionPurgesDebuffsAndBoostsPoise() {
    List<ActiveBuff> initialBuffs = new ArrayList<>();
    initialBuffs.add(ActiveBuff.builder()
        .id("bleed_debuff")
        .name("流血")
        .type(BuffType.DEBUFF)
        .category(BuffCategory.DOT)
        .remainingTicks(20)
        .build());
    initialBuffs.add(ActiveBuff.builder()
        .id("iron_skin_buff")
        .name("金剛不壞")
        .type(BuffType.BUFF)
        .category(BuffCategory.STAT_MODIFIER)
        .remainingTicks(40)
        .build());

    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-test")
        .name("守墓將軍")
        .rank(MobRank.BOSS)
        .hp(500)
        .maxHp(500)
        .poise(0)
        .maxPoise(100)
        .poiseBroken(true)
        .currentPhase(1)
        .maxPhases(2)
        .phaseThresholds(List.of(50))
        .stunnedUntil(System.currentTimeMillis() + 5000L)
        .activeBuffs(initialBuffs)
        .build();

    boss.setHp(200); // 跌破 50%
    assertThat(boss.checkPhaseTransition()).isEqualTo(2);

    boss.transitionToPhase(2);

    // 1. 負面 Debuff / DoT 已被淨化，良性 Buff 保留
    assertThat(boss.getActiveBuffs()).hasSize(1);
    assertThat(boss.getActiveBuffs().get(0).getId()).isEqualTo("iron_skin_buff");

    // 2. 眩暈解除
    assertThat(boss.isStunned()).isFalse();

    // 3. 罡氣破防解除，重聚並提升 25% 上限 (100 -> 125)
    assertThat(boss.isPoiseBroken()).isFalse();
    assertThat(boss.getMaxPoise()).isEqualTo(125);
    assertThat(boss.getPoise()).isEqualTo(125);
  }

  @Test
  @DisplayName("F2-02-T4: 驗證資料庫首領資料驅動配置 (mobs.json) 與前後端 DTO 鏡像投影完整性")
  void testDataLoadedAndDtoProjection() {
    var mobOpt = templateReader.findMob("mozhu_mines:boss_song_tianheng");
    assertThat(mobOpt).isPresent();
    MobTemplate tpl = mobOpt.get();

    // 驗證宋天衡具備 3 階段與 [70, 30] 閾值配置
    assertThat(tpl.maxPhases()).isEqualTo(3);
    assertThat(tpl.phaseThresholds()).containsExactly(70, 30);

    BattleEnemy enemy = BattleEnemy.fromTemplate("boss-1", tpl, RowPosition.FRONT, null, templateReader);
    assertThat(enemy).isNotNull();
    assertThat(enemy.getCurrentPhase()).isEqualTo(1);
    assertThat(enemy.getMaxPhases()).isEqualTo(3);
    assertThat(enemy.getPhaseThresholds()).containsExactly(70, 30);
    assertThat(enemy.isEnraged()).isFalse();

    // 模擬轉入狂暴階段投影至 DTO
    enemy.transitionToPhase(3);
    BattleEnemyDto dto = BattleEnemyDto.from(0, enemy);
    assertThat(dto.currentPhase()).isEqualTo(3);
    assertThat(dto.maxPhases()).isEqualTo(3);
    assertThat(dto.phaseThresholds()).containsExactly(70, 30);
    assertThat(dto.isEnraged()).isTrue();

    // 驗證 ViewDto
    BattleEnemyViewDto viewDto = new BattleEnemyViewDto(
        0, enemy.getId(), enemy.getName(), enemy.getHp(), enemy.getMaxHp(),
        enemy.getRow().name(), true, true, false, null, null, 0,
        false, 1, "CENTER", 1, 2, 1, 1, null, null, "PHYSICAL", "SINGLE",
        false, 0, 0, true, enemy.getPoise(), enemy.getMaxPoise(), false, 0,
        List.of(), "", 0, true,
        enemy.getCurrentPhase(), enemy.getMaxPhases(), enemy.getPhaseThresholds(), enemy.isEnraged()
    );
    assertThat(viewDto.currentPhase()).isEqualTo(3);
    assertThat(viewDto.isEnraged()).isTrue();
  }
}
