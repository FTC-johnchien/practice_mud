package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto;
import com.example.htmlmud.domain.party.model.RowPosition;

class EnemyIntentTest {

  @Test
  @DisplayName("ENEMY-INTENT-01: 測試敵人意圖預告啟動與清除")
  void testEnemyIntentLifecycle() {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-test-1")
        .name("幽冥狂屍")
        .hp(100)
        .maxHp(100)
        .row(RowPosition.FRONT)
        .build();

    assertThat(enemy.isCasting()).isFalse();
    assertThat(enemy.getCurrentIntentName()).isNull();

    // 啟動意圖
    enemy.startIntent("⚔️", "屍爪猛擊", "HEAVY", "SINGLE", 1200, true);

    assertThat(enemy.getCurrentIntentIcon()).isEqualTo("⚔️");
    assertThat(enemy.getCurrentIntentName()).isEqualTo("屍爪猛擊");
    assertThat(enemy.getCurrentIntentType()).isEqualTo("HEAVY");
    assertThat(enemy.getIntentTargetScope()).isEqualTo("SINGLE");
    assertThat(enemy.isCasting()).isTrue();
    assertThat(enemy.getCastDurationMs()).isEqualTo(1200);
    assertThat(enemy.getCastRemainingMs()).isGreaterThan(0).isLessThanOrEqualTo(1200);
    assertThat(enemy.isInterruptible()).isTrue();

    // 清除意圖
    enemy.clearIntent();
    assertThat(enemy.isCasting()).isFalse();
    assertThat(enemy.getCurrentIntentName()).isNull();
    assertThat(enemy.getCastRemainingMs()).isEqualTo(0);
  }

  @Test
  @DisplayName("ENEMY-INTENT-01: 測試 BattleEnemyViewDto 向後相容與意圖欄位封裝")
  void testBattleEnemyViewDtoBackwardsCompatibility() {
    // 舊版 19 參數構造
    BattleEnemyViewDto oldDto = new BattleEnemyViewDto(
        0, "mob-1", "血蝠", 50, 50, "FRONT", true, false, false,
        "member-1", "劍修", 10, false, 1, "CENTER", 1, 2, 1, 1
    );

    assertThat(oldDto.intentIcon()).isNull();
    assertThat(oldDto.intentName()).isNull();
    assertThat(oldDto.intentType()).isEqualTo("PHYSICAL");
    assertThat(oldDto.isCasting()).isFalse();
    assertThat(oldDto.isInterruptible()).isTrue();

    // 全參構造
    BattleEnemyViewDto fullDto = new BattleEnemyViewDto(
        0, "mob-1", "血蝠", 50, 50, "FRONT", true, false, false,
        "member-1", "劍修", 10, false, 1, "CENTER", 1, 2, 1, 1,
        "🔥", "烈火咒", "SPELL", "SINGLE", true, 1500L, 1200L, true
    );

    assertThat(fullDto.intentIcon()).isEqualTo("🔥");
    assertThat(fullDto.intentName()).isEqualTo("烈火咒");
    assertThat(fullDto.intentType()).isEqualTo("SPELL");
    assertThat(fullDto.isCasting()).isTrue();
    assertThat(fullDto.castRemainingMs()).isEqualTo(1200L);
  }
}
