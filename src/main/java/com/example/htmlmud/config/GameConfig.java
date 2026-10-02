package com.example.htmlmud.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 全域遊戲數值與規則設定檔 (Game Configuration Central Hub)
 * 集中管理原本散落在代碼中的初始數值、戰鬥常數、回復心跳與房間預設值。
 * 支援 application.yml 綁定與靜態回退存取。
 */
@Component
@ConfigurationProperties(prefix = "game")
public class GameConfig {

  private static volatile GameConfig INSTANCE;

  private Defaults defaults = new Defaults();
  private Combat combat = new Combat();
  private Regen regen = new Regen();

  public GameConfig() {
    INSTANCE = this;
  }

  @PostConstruct
  public void init() {
    INSTANCE = this;
  }

  /**
   * 取得全域單例 (用於非 Spring 管理的靜態工廠方法，如 Player.createSinglePlayer)
   */
  public static GameConfig getInstance() {
    if (INSTANCE == null) {
      synchronized (GameConfig.class) {
        if (INSTANCE == null) {
          INSTANCE = new GameConfig();
        }
      }
    }
    return INSTANCE;
  }

  public Defaults getDefaults() {
    return defaults;
  }

  public void setDefaults(Defaults defaults) {
    this.defaults = defaults;
  }

  public Combat getCombat() {
    return combat;
  }

  public void setCombat(Combat combat) {
    this.combat = combat;
  }

  public Regen getRegen() {
    return regen;
  }

  public void setRegen(Regen regen) {
    this.regen = regen;
  }

  // =========================================================================
  // 1. 預設值 (Defaults)
  // =========================================================================
  public static class Defaults {
    private PlayerDefaults player = new PlayerDefaults();

    public PlayerDefaults getPlayer() {
      return player;
    }

    public void setPlayer(PlayerDefaults player) {
      this.player = player;
    }
  }

  public static class PlayerDefaults {
    private int initialHp = 200;
    private int initialMp = 100;
    private int initialCoin = 100;
    private String spawnRoomId = "newbie_village:inn";
    private String respawnRoomId = "newbie_village:cemetery";

    public int getInitialHp() {
      return initialHp;
    }

    public void setInitialHp(int initialHp) {
      this.initialHp = initialHp;
    }

    public int getInitialMp() {
      return initialMp;
    }

    public void setInitialMp(int initialMp) {
      this.initialMp = initialMp;
    }

    public int getInitialCoin() {
      return initialCoin;
    }

    public void setInitialCoin(int initialCoin) {
      this.initialCoin = initialCoin;
    }

    public String getSpawnRoomId() {
      return spawnRoomId;
    }

    public void setSpawnRoomId(String spawnRoomId) {
      this.spawnRoomId = spawnRoomId;
    }

    public String getRespawnRoomId() {
      return respawnRoomId;
    }

    public void setRespawnRoomId(String respawnRoomId) {
      this.respawnRoomId = respawnRoomId;
    }
  }

  // =========================================================================
  // 2. 戰鬥數值 (Combat)
  // =========================================================================
  public static class Combat {
    private int defaultGcdMs = 1500;
    private double baseHitChance = 0.80;
    private double dexHitModifier = 0.01;
    private long unarmedAttackSpeedMs = 2000;
    private long multiAttackMinIntervalMs = 450;
    private long multiAttackMaxIntervalMs = 551;
    private double maxTotalDefenseChance = 0.75;
    private double minNormalHitChance = 0.05;

    public int getDefaultGcdMs() {
      return defaultGcdMs;
    }

    public void setDefaultGcdMs(int defaultGcdMs) {
      this.defaultGcdMs = defaultGcdMs;
    }

    public double getBaseHitChance() {
      return baseHitChance;
    }

    public void setBaseHitChance(double baseHitChance) {
      this.baseHitChance = baseHitChance;
    }

    public double getDexHitModifier() {
      return dexHitModifier;
    }

    public void setDexHitModifier(double dexHitModifier) {
      this.dexHitModifier = dexHitModifier;
    }

    public long getUnarmedAttackSpeedMs() {
      return unarmedAttackSpeedMs;
    }

    public void setUnarmedAttackSpeedMs(long unarmedAttackSpeedMs) {
      this.unarmedAttackSpeedMs = unarmedAttackSpeedMs;
    }

    public long getMultiAttackMinIntervalMs() {
      return multiAttackMinIntervalMs;
    }

    public void setMultiAttackMinIntervalMs(long multiAttackMinIntervalMs) {
      this.multiAttackMinIntervalMs = multiAttackMinIntervalMs;
    }

    public long getMultiAttackMaxIntervalMs() {
      return multiAttackMaxIntervalMs;
    }

    public void setMultiAttackMaxIntervalMs(long multiAttackMaxIntervalMs) {
      this.multiAttackMaxIntervalMs = multiAttackMaxIntervalMs;
    }

    public double getMaxTotalDefenseChance() {
      return maxTotalDefenseChance;
    }

    public void setMaxTotalDefenseChance(double maxTotalDefenseChance) {
      this.maxTotalDefenseChance = maxTotalDefenseChance;
    }

    public double getMinNormalHitChance() {
      return minNormalHitChance;
    }

    public void setMinNormalHitChance(double minNormalHitChance) {
      this.minNormalHitChance = minNormalHitChance;
    }
  }

  // =========================================================================
  // 3. 回復心跳 (Regen)
  // =========================================================================
  public static class Regen {
    private int tickModulo = 150;
    private double hpPercent = 0.05;
    private double mpPercent = 0.01;

    public int getTickModulo() {
      return tickModulo;
    }

    public void setTickModulo(int tickModulo) {
      this.tickModulo = tickModulo;
    }

    public double getHpPercent() {
      return hpPercent;
    }

    public void setHpPercent(double hpPercent) {
      this.hpPercent = hpPercent;
    }

    public double getMpPercent() {
      return mpPercent;
    }

    public void setMpPercent(double mpPercent) {
      this.mpPercent = mpPercent;
    }
  }
}
