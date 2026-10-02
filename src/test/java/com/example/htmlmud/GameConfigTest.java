package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.config.GameConfig;

class GameConfigTest {

  @Test
  @DisplayName("驗證 GameConfig 集中管理之全域數值與預設常數")
  void testGameConfigDefaults() {
    GameConfig config = GameConfig.getInstance();
    assertNotNull(config, "GameConfig 實例不應為空");

    // 1. 玩家預設值
    assertNotNull(config.getDefaults());
    assertNotNull(config.getDefaults().getPlayer());
    assertEquals(200, config.getDefaults().getPlayer().getInitialHp());
    assertEquals(100, config.getDefaults().getPlayer().getInitialMp());
    assertEquals(100, config.getDefaults().getPlayer().getInitialCoin());
    assertEquals("newbie_village:inn", config.getDefaults().getPlayer().getSpawnRoomId());
    assertEquals("newbie_village:cemetery", config.getDefaults().getPlayer().getRespawnRoomId());

    // 2. 戰鬥預設值
    assertNotNull(config.getCombat());
    assertEquals(1500, config.getCombat().getDefaultGcdMs());
    assertEquals(0.80, config.getCombat().getBaseHitChance(), 0.0001);
    assertEquals(0.01, config.getCombat().getDexHitModifier(), 0.0001);
    assertEquals(2000L, config.getCombat().getUnarmedAttackSpeedMs());
    assertEquals(450L, config.getCombat().getMultiAttackMinIntervalMs());
    assertEquals(551L, config.getCombat().getMultiAttackMaxIntervalMs());

    // 3. 回復心跳預設值
    assertNotNull(config.getRegen());
    assertEquals(150, config.getRegen().getTickModulo());
    assertEquals(0.05, config.getRegen().getHpPercent(), 0.0001);
    assertEquals(0.01, config.getRegen().getMpPercent(), 0.0001);
  }
}
