package com.example.htmlmud.domain.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.example.htmlmud.config.GameConfig;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseResolution;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.enums.BuffCategory;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.dungeon.battle.ActiveBuff;


/**
 * 跨系統核心戰鬥規則與純函式數學演算法 (UNIFY-03 CombatRules SSoT)
 * 提供 MUD 普攻/招式與 DRPG 小隊戰鬥之統一傷害、抗性折減與圓桌防禦計算。
 */
public final class CombatRules {

  private CombatRules() {
    // 工具類純函式，禁止實例化
  }

  /**
   * 基礎原始傷害計算 (武器物理/法術基值 - 目標護甲防禦，加浮動與怪物階級倍率)
   */
  public static int calculateRawDamage(int minDamage, int maxDamage, int defenderDefense, MobRank rank, double variance) {
    int min = Math.max(1, minDamage);
    int max = Math.max(min, maxDamage);
    int base = ThreadLocalRandom.current().nextInt(min, max + 1);
    int net = Math.max(1, base - defenderDefense);
    int finalDmg = (int) Math.round(net * variance);
    if (rank != null) {
      finalDmg = (int) Math.round(finalDmg * rank.getDamageMultiplier());
    }
    return Math.max(1, finalDmg);
  }

  /**
   * 基於 Combatant 抽象之原始傷害計算
   */
  public static int calculateRawDamage(Combatant attacker, Combatant defender, double variance) {
    if (attacker == null || defender == null) return 1;
    return calculateRawDamage(
        attacker.getEffectiveMinDamage(),
        attacker.getEffectiveMaxDamage(),
        defender.getEffectiveDefense(),
        attacker.getRank(),
        variance
    );
  }

  /**
   * 結算攻擊力竭破防 (Poise Broken / Stagger) 之敵怪加成 (F2-02)：
   * 若目標處於破防狀態，享受 1.5 倍致命暴擊傷害倍率。
   */
  public static int applyPoiseBreakCrit(int rawDamage, boolean isPoiseBroken) {
    if (!isPoiseBroken) return rawDamage;
    return Math.max(1, (int) Math.round(rawDamage * 1.5));
  }

  /**
   * 九大天道靈氣複合連鎖反應類型 (F2-05)
   */
  public enum ElementalReaction {
    NONE("", "", 1.0, 0),
    VAPORIZE("【氣化爆裂】", "💥 冰火相衝，水汽蒸騰引發劇烈爆鳴！", 2.0, 20),
    ELECTRO_CHARGED("【感電傳導】", "⚡ 雷水激盪，狂暴電弧跳躍傳導！", 1.5, 10),
    CHAOS_ANNIHILATION("【太極混沌湮滅】", "☯️ 陰陽交泰，太極混沌強制湮滅破罡！", 1.5, 50),
    COMBUSTION("【三昧燎原】", "🔥 烈火遇毒，三昧真火爆裂灼燒！", 1.8, 15);

    private final String title;
    private final String description;
    private final double damageMultiplier;
    private final int bonusPoiseDamage;

    ElementalReaction(String title, String description, double damageMultiplier, int bonusPoiseDamage) {
      this.title = title;
      this.description = description;
      this.damageMultiplier = damageMultiplier;
      this.bonusPoiseDamage = bonusPoiseDamage;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public int getBonusPoiseDamage() { return bonusPoiseDamage; }
  }

  /**
   * 元素複合連鎖反應結算結果 (F2-05)
   */
  public record ElementalReactionResult(
      ElementalReaction reaction,
      int damage,
      int poiseDamage,
      long stunDurationMs,
      boolean chainToRow,
      String logMessage,
      String consumedBuffId
  ) {
    public boolean hasReaction() {
      return reaction != null && reaction != ElementalReaction.NONE;
    }

    public static ElementalReactionResult none(int damage, int basePoiseDamage) {
      return new ElementalReactionResult(ElementalReaction.NONE, damage, basePoiseDamage, 0L, false, null, null);
    }
  }

  /**
   * 結算九大天道靈氣複合連鎖反應 (F2-05)
   * 當攻擊元素與目標身上已有的靈氣狀態碰撞時，觸發元素反應質變。
   */
  public static ElementalReactionResult evaluateElementalReaction(
      DamageType incomingType,
      int rawDamage,
      int basePoiseDamage,
      List<ActiveBuff> targetBuffs
  ) {
    if (incomingType == null || targetBuffs == null || targetBuffs.isEmpty() || rawDamage <= 0) {
      return ElementalReactionResult.none(rawDamage, basePoiseDamage);
    }

    for (ActiveBuff buff : targetBuffs) {
      if (buff == null || buff.isExpired()) continue;
      String el = buff.getElement();
      if (el == null || el.isBlank() || "NONE".equalsIgnoreCase(el)) continue;
      String upperEl = el.toUpperCase();

      // 1. 火 + 冰/水 -> 【氣化爆裂 (Vaporize)】
      if ((incomingType == DamageType.FIRE && ("ICE".equals(upperEl) || "WATER".equals(upperEl)))
          || ((incomingType == DamageType.ICE || incomingType == DamageType.WATER) && "FIRE".equals(upperEl))) {
        var r = ElementalReaction.VAPORIZE;
        int dmg = Math.max(1, (int) Math.round(rawDamage * r.getDamageMultiplier()));
        int pDmg = basePoiseDamage + r.getBonusPoiseDamage();
        String log = "\u001B[1;33m" + r.getTitle() + r.getDescription() + " 造成 " + dmg + " 點穿透打擊！\u001B[0m";
        return new ElementalReactionResult(r, dmg, pDmg, 0L, false, log, buff.getId());
      }

      // 2. 雷 + 水/冰 -> 【感電傳導 (Electro-Charged)】
      if ((incomingType == DamageType.LIGHTNING && ("WATER".equals(upperEl) || "ICE".equals(upperEl)))
          || ((incomingType == DamageType.WATER || incomingType == DamageType.ICE) && "LIGHTNING".equals(upperEl))) {
        var r = ElementalReaction.ELECTRO_CHARGED;
        int dmg = Math.max(1, (int) Math.round(rawDamage * r.getDamageMultiplier()));
        int pDmg = basePoiseDamage + r.getBonusPoiseDamage();
        String log = "\u001B[1;36m" + r.getTitle() + r.getDescription() + " 電弧跳躍，造成 " + dmg + " 點連鎖打擊！\u001B[0m";
        return new ElementalReactionResult(r, dmg, pDmg, 800L, true, log, buff.getId());
      }

      // 3. 陰煞/暗 + 純陽/光 -> 【太極混沌湮滅 (Chaos Annihilation)】
      if (((incomingType == DamageType.HOLY || incomingType == DamageType.LIGHT) && ("DARK".equals(upperEl) || "CORRUPT".equals(upperEl)))
          || ((incomingType == DamageType.DARK || incomingType == DamageType.CORRUPT) && ("HOLY".equals(upperEl) || "LIGHT".equals(upperEl)))) {
        var r = ElementalReaction.CHAOS_ANNIHILATION;
        int dmg = Math.max(1, (int) Math.round(rawDamage * r.getDamageMultiplier()));
        int pDmg = basePoiseDamage + r.getBonusPoiseDamage();
        String log = "\u001B[1;35m" + r.getTitle() + r.getDescription() + " 強制削減 " + r.getBonusPoiseDamage() + " 點罡氣，造成 " + dmg + " 點湮滅重創！\u001B[0m";
        return new ElementalReactionResult(r, dmg, pDmg, 0L, false, log, buff.getId());
      }

      // 4. 火 + 毒/木 -> 【三昧燎原 (Combustion)】
      if ((incomingType == DamageType.FIRE && ("POISON".equals(upperEl) || "WOOD".equals(upperEl)))
          || ((incomingType == DamageType.POISON || incomingType == DamageType.WOOD) && "FIRE".equals(upperEl))) {
        var r = ElementalReaction.COMBUSTION;
        int dmg = Math.max(1, (int) Math.round(rawDamage * r.getDamageMultiplier()));
        int pDmg = basePoiseDamage + r.getBonusPoiseDamage();
        String log = "\u001B[1;31m" + r.getTitle() + r.getDescription() + " 毒煞燃爆，造成 " + dmg + " 點劇烈烈焰衝擊！\u001B[0m";
        return new ElementalReactionResult(r, dmg, pDmg, 0L, false, log, buff.getId());
      }
    }

    return ElementalReactionResult.none(rawDamage, basePoiseDamage);
  }

  /**
   * 裂魂印記 (Primer) 與引爆戰技 (Detonator) 結算結果 (F2-05)
   */
  public record DetonationResult(
      boolean triggered,
      int singleTargetDamage,
      int aoeDamage,
      int bonusPoiseDamage,
      List<String> consumedBuffIds,
      String logMessage
  ) {
    public static DetonationResult none() {
      return new DetonationResult(false, 0, 0, 0, List.of(), null);
    }
  }

  /**
   * 結算裂魂印記與引爆戰技之連鎖引爆 (F2-05)
   * 命中帶有 PRIMER 印記的目標時，瞬間提早結算目標體內所有 DoT 剩餘總傷害，
   * 並將其轉化為劇烈單體爆發與全場 AOE 衝擊波。
   */
  public static DetonationResult evaluateDetonation(
      boolean isDetonator,
      int baseDetonationBonus,
      List<ActiveBuff> targetBuffs
  ) {
    if (!isDetonator || targetBuffs == null || targetBuffs.isEmpty()) {
      return DetonationResult.none();
    }

    // 尋找 PRIMER 印記
    ActiveBuff primerBuff = null;
    for (ActiveBuff b : targetBuffs) {
      if (b == null || b.isExpired()) continue;
      if (b.getCategory() == BuffCategory.PRIMER
          || (b.getEffectType() != null && "PRIMER".equalsIgnoreCase(b.getEffectType().name()))
          || (b.getId() != null && b.getId().toLowerCase().contains("primer"))) {
        primerBuff = b;
        break;
      }
    }

    if (primerBuff == null) {
      return DetonationResult.none();
    }

    List<String> consumedBuffIds = new ArrayList<>();
    consumedBuffIds.add(primerBuff.getId());

    int totalDotDamage = 0;
    for (ActiveBuff b : targetBuffs) {
      if (b == null || b.isExpired()) continue;
      if (b.getCategory() == BuffCategory.DOT) {
        int interval = Math.max(1, b.getTickIntervalTicks());
        int remainingJumps = Math.max(1, (b.getRemainingTicks() + interval - 1) / interval);
        int dotTotal = b.getValue() * Math.max(1, b.getStacks()) * remainingJumps;
        totalDotDamage += dotTotal;
        consumedBuffIds.add(b.getId());
      }
    }

    int primerBase = Math.max(30, primerBuff.getValue() * Math.max(1, primerBuff.getStacks()));
    int singleTargetDamage = totalDotDamage + primerBase + Math.max(0, baseDetonationBonus);
    int aoeDamage = (int) Math.round(singleTargetDamage * 0.5);
    int bonusPoiseDamage = 35;

    String log = "\u001B[1;31m💥【裂魂印爆】引爆戰技擊碎【" + primerBuff.getName() + "】！提前結算 "
        + totalDotDamage + " 點殘存毒煞，瞬間爆發 " + singleTargetDamage + " 點重創並掀起 "
        + aoeDamage + " 點全場震盪衝擊波！\u001B[0m";

    return new DetonationResult(true, singleTargetDamage, aoeDamage, bonusPoiseDamage, consumedBuffIds, log);
  }

  /**
   * 抗性折減與逆流回血結算結果 (BUFF-01 & UNIFY-03)

   */
  public record ResistanceResult(int damage, int heal) {}

  /**
   * 傷害類型抗性折減與五行逆流回血契約 (DamageType & resistances)
   * 1. 物理傷害 (PHYSICAL/SLASH/PIERCE/BLUNT)：不走元素抗性回血，直接回傳傷害。
   * 2. 五行法術/陰煞傷害：若抗性 > 1.0 (100%)，超額部分轉化為氣血回復 (順成人，逆成仙)。
   *    例如火抗 1.25 -> 25% 吸收回血，承受 0 點傷害。
   *    若抗性 <= 1.0，依 (1.0 - resistance) 折減傷害。
   */
  public static ResistanceResult resolveResistance(int rawDamage, DamageType type, double resistance) {
    if (rawDamage <= 0) return new ResistanceResult(0, 0);

    // 1. 物理傷害：依常規護甲防禦折減 (保底 10% 穿透防止絕對無敵)，不走元素逆流回血
    if (type == DamageType.PHYSICAL || (type != null && type.is(DamageType.PHYSICAL))) {
      int finalDmg = (resistance != 0.0)
          ? Math.max(1, (int) Math.round(rawDamage * Math.max(0.1, 1.0 - resistance)))
          : Math.max(1, rawDamage);
      return new ResistanceResult(finalDmg, 0);
    }

    // 2. 五行法術/陰煞傷害：若抗性 > 1.0 (100%)，超額部分轉化為氣血回復 (順成人，逆成仙)
    if (resistance > 1.0) {
      double absorbRatio = resistance - 1.0;
      int healAmount = (int) Math.round(rawDamage * absorbRatio);
      return new ResistanceResult(0, healAmount);
    } else {
      int finalDmg = (int) Math.round(rawDamage * Math.max(0.0, 1.0 - resistance));
      return new ResistanceResult(Math.max(0, finalDmg), 0);
    }
  }

  /**
   * 傷害類型抗性折減 (向下相容介面)
   */
  public static int applyResistance(int rawDamage, DamageType type, double resistance) {
    return resolveResistance(rawDamage, type, resistance).damage();
  }

  /**
   * 戰鬥防禦參數聚合封裝 (用於純函式傳參)
   */
  public record DefenseParams(
      String attackerName,
      String defenderName,
      String weaponName,
      String defWeapon,
      int atkDex,
      int defDex,
      int defStr,
      int defCon,
      SkillTemplate dodgeSkill,
      SkillTemplate parrySkill,
      SkillTemplate forceSkill,
      boolean hasShield,
      int shieldBonusDef,
      String shieldName,
      int rawDamage,
      String enemyMove,
      CombatResourceType resType,
      boolean canRiposte,
      int riposteAttackPower,
      com.example.htmlmud.domain.model.entity.LivingStats defStats,
      GameConfig gameConfig
  ) {}

  /**
   * 純函式一元一次擲骰圓桌判定 (One-Roll Combat Table)
   */
  public static DefenseResolution executeCombatTable(DefenseParams p, Double predeterminedRoll) {
    // 檢查防禦者精力狀態 (Stamina / Poise Break)
    int currentStamina = (p.defStats() != null) ? p.defStats().getStamina() : 100;
    boolean poiseBroken = (currentStamina <= 0);

    // 1. Miss 閾值 (機率)：BaseMiss(5%) + max(0, (Def.DEX - Atk.DEX) * 0.5%)
    double missChance = Math.min(0.25, Math.max(0.02, 0.05 + Math.max(0, p.defDex() - p.atkDex()) * 0.005));

    // 2. Dodge 閾值 (機率)：Skill.dodgeRate + (Def.DEX * 0.8%) - (Atk.DEX * 0.3%)
    double dodgeChance = 0.0;
    if (!poiseBroken && p.dodgeSkill() != null) {
      double baseDodge = 0.10;
      if (p.dodgeSkill().getMechanics() != null) {
        if (p.dodgeSkill().getMechanics().dodgeRate() > 0) {
          baseDodge = p.dodgeSkill().getMechanics().dodgeRate();
        } else if (p.dodgeSkill().getMechanics().dodgeMod() > 0) {
          baseDodge = p.dodgeSkill().getMechanics().dodgeMod();
        }
      }
      dodgeChance = Math.min(0.75, Math.max(0.05, baseDodge + (p.defDex() * 0.008) - (p.atkDex() * 0.003)));
    }

    // 3. Parry 閾值 (機率)：Skill.parryRate + (Def.STR * 0.4%) + (Def.DEX * 0.4%)
    double parryChance = 0.0;
    double reduceRatio = 0.50;
    if (!poiseBroken && p.parrySkill() != null) {
      double baseParry = 0.15;
      if (p.parrySkill().getMechanics() != null) {
        if (p.parrySkill().getMechanics().parryRate() > 0) {
          baseParry = p.parrySkill().getMechanics().parryRate();
        } else if (p.parrySkill().getMechanics().parryMod() > 0) {
          baseParry = p.parrySkill().getMechanics().parryMod();
        }
        if (p.parrySkill().getMechanics().damageReduce() > 0) {
          double extraReduce = p.parrySkill().getMechanics().damageReduce();
          reduceRatio = Math.max(0.20, 0.50 - extraReduce);
        }
      }
      parryChance = Math.min(0.65, Math.max(0.05, baseParry + (p.defStr() * 0.004) + (p.defDex() * 0.004)));
    }

    // 4. Block 閾值 (機率)
    double blockChance = 0.0;
    if (p.hasShield()) {
      blockChance = Math.min(0.60, Math.max(0.10, 0.20 + (p.defCon() * 0.003)));
    }

    // 5. Crit 閾值 (機率)
    double critChance = Math.min(0.35, Math.max(0.05, 0.05 + (p.atkDex() * 0.002)));

    // 機率安全收斂
    double maxTotalDef = (p.gameConfig() != null && p.gameConfig().getCombat() != null)
        ? p.gameConfig().getCombat().getMaxTotalDefenseChance()
        : 0.75;
    double minNormalHit = (p.gameConfig() != null && p.gameConfig().getCombat() != null)
        ? p.gameConfig().getCombat().getMinNormalHitChance()
        : 0.05;

    double totalDef = missChance + dodgeChance + parryChance + blockChance;
    if (totalDef > maxTotalDef) {
      double scale = maxTotalDef / totalDef;
      missChance *= scale;
      dodgeChance *= scale;
      parryChance *= scale;
      blockChance *= scale;
      totalDef = maxTotalDef;
    }

    double remainingPool = Math.max(0.0, 1.0 - totalDef);
    double maxCritAllowed = Math.max(0.01, remainingPool - minNormalHit);
    double actualCrit = Math.min(critChance, maxCritAllowed);

    // 擲骰判定
    double roll = (predeterminedRoll != null)
        ? predeterminedRoll
        : ThreadLocalRandom.current().nextDouble();

    double missLimit = missChance;
    double dodgeLimit = missLimit + dodgeChance;
    double parryLimit = dodgeLimit + parryChance;
    double blockLimit = parryLimit + blockChance;
    double critLimit = blockLimit + actualCrit;

    int forceReduction = 0;
    if (p.forceSkill() != null && p.forceSkill().getMechanics() != null && p.forceSkill().getMechanics().defenseMod() > 0) {
      forceReduction = p.forceSkill().getMechanics().defenseMod();
    }

    if (roll < missLimit) {
      String missLog = "\u001B[1;30m💨【未命中】" + p.attackerName() + " 攻勢落空，未能觸及 " + p.defenderName() + "！\u001B[0m";
      return new DefenseResolution(DefenseOutcome.MISS, 0, 0, 0, missLog, false, 0, 0, poiseBroken);
    }

    if (roll < dodgeLimit) {
      int staminaCost = 2;
      if (p.defStats() != null) {
        p.defStats().setStamina(Math.max(0, p.defStats().getStamina() - staminaCost));
      }
      String msg = extractDodgeMessage(p.dodgeSkill(), p.attackerName(), p.defenderName(), p.weaponName());
      String log = "\u001B[1;36m💨【身法閃避】" + msg + "\u001B[0m";
      return new DefenseResolution(DefenseOutcome.DODGED, 0, 15, 0, log, false, 0, staminaCost, false);
    }

    if (roll < parryLimit) {
      int staminaCost = 3;
      if (p.defStats() != null) {
        p.defStats().setStamina(Math.max(0, p.defStats().getStamina() - staminaCost));
      }
      int parriedDmg = Math.max(1, (int) Math.round(p.rawDamage() * reduceRatio));
      String msg = extractParryMessage(p.parrySkill(), p.attackerName(), p.defenderName(), p.weaponName(), p.defWeapon());
      String log = "\u001B[1;33m🛡️【招架格擋】" + msg + "（傷害減免至 " + parriedDmg + " 點）\u001B[0m";

      boolean triggeredRiposte = false;
      int riposteDmg = 0;
      if (p.canRiposte()) {
        boolean shouldTrigger = (predeterminedRoll != null) || (ThreadLocalRandom.current().nextDouble() < 0.50);
        if (shouldTrigger) {
          triggeredRiposte = true;
          riposteDmg = Math.max(3, p.riposteAttackPower());
          log += "\r\n\u001B[1;36m⚔️【破招反擊】" + p.defenderName() + " 借力打力，反手一記突刺，對 " + p.attackerName() + " 造成了 " + riposteDmg + " 點反擊傷害！\u001B[0m";
        }
      }

      return new DefenseResolution(DefenseOutcome.PARRIED, parriedDmg, 5, 5, log, triggeredRiposte, riposteDmg, staminaCost, false);
    }

    if (roll < blockLimit) {
      int shieldBlockValue = Math.max(5, (p.shieldBonusDef() * 2) + (p.defCon() / 2));
      int blockedDmg = Math.max(1, p.rawDamage() - shieldBlockValue);
      if (poiseBroken) {
        blockedDmg = Math.max(1, (int) Math.round(blockedDmg * 1.20));
      }
      String log = "\u001B[1;33m🛡️【盾牌格擋】" + p.defenderName() + " 舉起【" + p.shieldName() + "】固若金湯，化解了 " + shieldBlockValue + " 點衝擊！（承受 " + blockedDmg + " 點傷害）\u001B[0m";
      if (poiseBroken) {
        log += "\r\n\u001B[1;35m⚠️【架勢破防】" + p.defenderName() + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
      }
      return new DefenseResolution(DefenseOutcome.BLOCKED, blockedDmg, 5, 5, log, false, 0, 0, poiseBroken);
    }

    if (roll < critLimit) {
      int critDmg = Math.max(1, (int) Math.round(p.rawDamage() * 1.5) - forceReduction);
      if (poiseBroken) {
        critDmg = Math.max(1, (int) Math.round(critDmg * 1.20));
      }
      String critLog = "\u001B[1;31m💥【致命一擊】" + p.attackerName() + " 破開空隙正中要害，對 " + p.defenderName() + " 爆擊造成 " + critDmg + " 點毀滅傷害！\u001B[0m";
      if (poiseBroken) {
        critLog += "\r\n\u001B[1;35m⚠️【架勢破防】" + p.defenderName() + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
      }
      return new DefenseResolution(DefenseOutcome.CRIT, critDmg, 10, 15, critLog, false, 0, 0, poiseBroken);
    }

    int finalDmg = Math.max(1, p.rawDamage() - forceReduction);
    if (poiseBroken) {
      finalDmg = Math.max(1, (int) Math.round(finalDmg * 1.20));
    }
    int rage = (p.resType() == CombatResourceType.RAGE) ? 15 : 0;
    String hitLog;
    if (p.enemyMove() != null && !p.enemyMove().isBlank()) {
      hitLog = "\u001B[1;31m⚡【" + p.attackerName() + "】施展【" + p.enemyMove() + "】，重創 " + p.defenderName() + " 造成 " + finalDmg + " 點傷害！\u001B[0m";
    } else {
      hitLog = "\u001B[1;31m⚡【" + p.attackerName() + "】發起猛烈撲擊，重創 " + p.defenderName() + " 造成 " + finalDmg + " 點傷害！\u001B[0m";
    }
    if (poiseBroken) {
      hitLog += "\r\n\u001B[1;35m⚠️【架勢破防】" + p.defenderName() + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
    }

    return new DefenseResolution(DefenseOutcome.HIT, finalDmg, 10, rage, hitLog, false, 0, 0, poiseBroken);
  }

  public static String extractDodgeMessage(SkillTemplate skill, String attacker, String defender, String weapon) {
    if (skill != null) {
      if (skill.getMessages() != null && skill.getMessages().containsKey("dodge")) {
        Object dodgeObj = skill.getMessages().get("dodge");
        if (dodgeObj instanceof List<?> list && !list.isEmpty()) {
          int idx = ThreadLocalRandom.current().nextInt(list.size());
          return replacePlaceholders(String.valueOf(list.get(idx)), attacker, defender, weapon, null);
        } else if (dodgeObj instanceof String str && !str.isBlank()) {
          return replacePlaceholders(str, attacker, defender, weapon, null);
        }
      }
      if (skill.getMoves() != null && !skill.getMoves().isEmpty()) {
        int idx = ThreadLocalRandom.current().nextInt(skill.getMoves().size());
        MoveAction move = skill.getMoves().get(idx);
        if (move != null && move.msg() != null && move.msg().success() != null && !move.msg().success().isBlank()) {
          return replacePlaceholders(move.msg().success(), attacker, defender, weapon, null);
        }
      }
    }
    return defender + " 施展【" + (skill != null ? skill.getName() : "身法") + "】，輕靈躍起避開了 " + attacker + " 的凌厲攻勢！";
  }

  public static String extractParryMessage(SkillTemplate skill, String attacker, String defender, String weapon, String defWeapon) {
    if (defWeapon == null || defWeapon.isBlank()) {
      defWeapon = "兵刃";
    }
    if (skill != null) {
      if (skill.getMessages() != null && skill.getMessages().containsKey("parried")) {
        Object parriedObj = skill.getMessages().get("parried");
        if (parriedObj instanceof List<?> list && !list.isEmpty()) {
          int idx = ThreadLocalRandom.current().nextInt(list.size());
          return replacePlaceholders(String.valueOf(list.get(idx)), attacker, defender, weapon, defWeapon);
        } else if (parriedObj instanceof String str && !str.isBlank()) {
          return replacePlaceholders(str, attacker, defender, weapon, defWeapon);
        }
      }
      if (skill.getMoves() != null && !skill.getMoves().isEmpty()) {
        int idx = ThreadLocalRandom.current().nextInt(skill.getMoves().size());
        MoveAction move = skill.getMoves().get(idx);
        if (move != null && move.msg() != null && move.msg().success() != null && !move.msg().success().isBlank()) {
          return replacePlaceholders(move.msg().success(), attacker, defender, weapon, defWeapon);
        }
      }
    }
    return defender + " 運起【" + (skill != null ? skill.getName() : "護體招架") + "】，穩穩架住了 " + attacker + " 的猛攻！";
  }

  public static String replacePlaceholders(String template, String attacker, String defender, String weapon, String defWeapon) {
    if (template == null) return "";
    String res = template;
    res = res.replace("$N", attacker != null ? attacker : "敵人");
    res = res.replace("$n", defender != null ? defender : "自身");
    res = res.replace("$w", weapon != null ? weapon : "兵刃");
    res = res.replace("$W", defWeapon != null ? defWeapon : "兵刃");
    res = res.replace("$l", "要害");
    return res;
  }
}
