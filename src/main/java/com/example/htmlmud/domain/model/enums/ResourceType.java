package com.example.htmlmud.domain.model.enums;

import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResourceType {

  // === 核心屬性 (直接操作變數) ===
  // 邏輯: (讀取方法, 扣除邏輯)
  @JsonProperty("hp") @JsonAlias({"HP"})
  HP(LivingStats::getHp, (s, val) -> s.setHp(s.getHp() - val)),

  @JsonProperty("mp") @JsonAlias({"MP"})
  MP(LivingStats::getMp, (s, val) -> s.setMp(s.getMp() - val)),

  @JsonProperty("stamina") @JsonAlias({"STAMINA"})
  STAMINA(LivingStats::getStamina, (s, val) -> s.setStamina(s.getStamina() - val)),

  @JsonProperty("age") @JsonAlias({"AGE"})
  AGE(LivingStats::getAge, (s, val) -> s.setAge(s.getAge() - val)),

  @JsonProperty("coin") @JsonAlias({"COIN", "money", "MONEY"})
  COIN(LivingStats::getCoin, (s, val) -> s.setAge(s.getCoin() - val)),

  @JsonProperty("exp") @JsonAlias({"EXP"})
  EXP(LivingStats::getExp, (s, val) -> s.setExp(s.getExp() - val)),

  @JsonProperty("combat_exp") @JsonAlias({"COMBAT_EXP"})
  COMBAT_EXP(LivingStats::getCombatExp, (s, val) -> s.setCombatExp(s.getCombatExp() - val)),

  @JsonProperty("potential") @JsonAlias({"POTENTIAL"})
  POTENTIAL(LivingStats::getPotential, (s, val) -> s.setPotential(s.getPotential() - val)),

  @JsonProperty("str") @JsonAlias({"STR"})
  STR(LivingStats::getStr, (s, val) -> s.setStr(s.getStr() - val)),

  @JsonProperty("int") @JsonAlias({"INT", "intelligence", "INTELLIGENCE"})
  INTELLIGENCE(LivingStats::getIntelligence,
      (s, val) -> s.setIntelligence(s.getIntelligence() - val)),

  @JsonProperty("dex") @JsonAlias({"DEX"})
  DEX(LivingStats::getDex, (s, val) -> s.setDex(s.getDex() - val)),

  @JsonProperty("con") @JsonAlias({"CON"})
  CON(LivingStats::getCon, (s, val) -> s.setCon(s.getCon() - val)),



  // === 特殊資源 (操作 Map) ===
  // 這裡我們直接傳入 Map 的 Key 字串，透過 lambda 呼叫 modifyCombatResource
  // 注意：這裡 val 是傳入要扣除的值，我們傳 -val 給 modify 方法
  @JsonProperty("charge")
  CHARGE(s -> s.getCombatResource("charge"), (s, val) -> s.modifyCombatResource("charge", -val)),

  @JsonProperty("combo")
  COMBO(s -> s.getCombatResource("combo"), (s, val) -> s.modifyCombatResource("combo", -val));



  private final ToIntFunction<LivingStats> getter;
  private final ObjIntConsumer<LivingStats> deducter;

  /**
   * Enum 建構子
   *
   * @param getter 如何取得數值 (Function)
   * @param deducter 如何扣除數值 (Consumer)
   */
  ResourceType(ToIntFunction<LivingStats> getter, ObjIntConsumer<LivingStats> deducter) {
    this.getter = getter;
    this.deducter = deducter;
  }

  // 對外公開的方法
  public int getCurrent(LivingStats actor) {
    return getter.applyAsInt(actor);
  }

  public void deduct(LivingStats actor, int amount) {
    deducter.accept(actor, amount);
  }
}
