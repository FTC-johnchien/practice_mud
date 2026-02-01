package com.example.htmlmud.domain.model;

import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResourceType {

  // === 核心屬性 (直接操作變數) ===
  // 邏輯: (讀取方法, 扣除邏輯)
  @JsonProperty("hp") @JsonAlias({"HP"})
  HP(LivingState::getHp, (s, val) -> s.setHp(s.getHp() - val)),

  @JsonProperty("mp") @JsonAlias({"MP"})
  MP(LivingState::getMp, (s, val) -> s.setMp(s.getMp() - val)),

  @JsonProperty("stamina") @JsonAlias({"STAMINA"})
  STAMINA(LivingState::getStamina, (s, val) -> s.setStamina(s.getStamina() - val)),

  @JsonProperty("age") @JsonAlias({"AGE"})
  AGE(LivingState::getAge, (s, val) -> s.setAge(s.getAge() - val)),

  @JsonProperty("coin") @JsonAlias({"COIN", "money", "MONEY"})
  COIN(LivingState::getCoin, (s, val) -> s.setAge(s.getCoin() - val)),

  @JsonProperty("exp") @JsonAlias({"EXP"})
  EXP(LivingState::getExp, (s, val) -> s.setExp(s.getExp() - val)),

  @JsonProperty("combat_exp") @JsonAlias({"COMBAT_EXP"})
  COMBAT_EXP(LivingState::getCombatExp, (s, val) -> s.setCombatExp(s.getCombatExp() - val)),

  @JsonProperty("potential") @JsonAlias({"POTENTIAL"})
  POTENTIAL(LivingState::getPotential, (s, val) -> s.setPotential(s.getPotential() - val)),

  @JsonProperty("str") @JsonAlias({"STR"})
  STR(LivingState::getStr, (s, val) -> s.setStr(s.getStr() - val)),

  @JsonProperty("int") @JsonAlias({"INT", "intelligence", "INTELLIGENCE"})
  INTELLIGENCE(LivingState::getIntelligence,
      (s, val) -> s.setIntelligence(s.getIntelligence() - val)),

  @JsonProperty("dex") @JsonAlias({"DEX"})
  DEX(LivingState::getDex, (s, val) -> s.setDex(s.getDex() - val)),

  @JsonProperty("con") @JsonAlias({"CON"})
  CON(LivingState::getCon, (s, val) -> s.setCon(s.getCon() - val)),



  // === 特殊資源 (操作 Map) ===
  // 這裡我們直接傳入 Map 的 Key 字串，透過 lambda 呼叫 modifyCombatResource
  // 注意：這裡 val 是傳入要扣除的值，我們傳 -val 給 modify 方法
  @JsonProperty("charge")
  CHARGE(s -> s.getCombatResource("charge"), (s, val) -> s.modifyCombatResource("charge", -val)),

  @JsonProperty("combo")
  COMBO(s -> s.getCombatResource("combo"), (s, val) -> s.modifyCombatResource("combo", -val));



  private final ToIntFunction<LivingState> getter;
  private final ObjIntConsumer<LivingState> deducter;

  /**
   * Enum 建構子
   *
   * @param getter 如何取得數值 (Function)
   * @param deducter 如何扣除數值 (Consumer)
   */
  ResourceType(ToIntFunction<LivingState> getter, ObjIntConsumer<LivingState> deducter) {
    this.getter = getter;
    this.deducter = deducter;
  }

  // 對外公開的方法
  public int getCurrent(LivingState actor) {
    return getter.applyAsInt(actor);
  }

  public void deduct(LivingState actor, int amount) {
    deducter.accept(actor, amount);
  }
}
