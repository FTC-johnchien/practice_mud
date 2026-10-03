package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.htmlmud.config.GameConfig;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;

/**
 * 戰鬥防禦與被動心法檢定解算器 (Generic Defense & Passive Resolver)
 * 負責小隊成員在遭受攻擊時的一元一次擲骰圓桌判定 (One-Roll Combat Table)：
 * [Miss] -> [Dodge] -> [Parry] -> [Block] -> [Crit] -> [Normal Hit]
 * 遵循「機制運行 + 資料驅動」原則：絕不寫死技能 ID，所有係數與文本全從 SkillTemplate 抽取。
 */
@Component
public class DefenseResolver {

  public enum DefenseOutcome {
    HIT,      // 普通命中受創
    CRIT,     // 致命一擊 (暴擊受創 1.5x)
    BLOCKED,  // 盾牌格擋成功 (扣除盾牌格擋值)
    PARRIED,  // 招架格擋成功 (傷害減免 50%~70%, +5 SP, +5 怒氣)
    DODGED,   // 身法閃避成功 (0傷害, +15 SP)
    MISS      // 未命中 (0傷害)
  }

  public record DefenseResolution(
      DefenseOutcome outcome,
      int finalDamage,
      int spGained,
      int rageGained,
      String combatLog,
      boolean riposteTriggered,
      int riposteDamage,
      int staminaConsumed,
      boolean poiseBroken
  ) {
    public DefenseResolution(DefenseOutcome outcome, int finalDamage, int spGained, int rageGained, String combatLog) {
      this(outcome, finalDamage, spGained, rageGained, combatLog, false, 0, 0, false);
    }

    public DefenseResolution(DefenseOutcome outcome, int finalDamage, int spGained, int rageGained, String combatLog, boolean riposteTriggered, int riposteDamage) {
      this(outcome, finalDamage, spGained, rageGained, combatLog, riposteTriggered, riposteDamage, 0, false);
    }
  }

  private final GameConfig gameConfig;
  private final com.example.htmlmud.domain.repository.TemplateReader templateReader;

  public DefenseResolver() {
    this(null, new com.example.htmlmud.domain.service.TemplateCatalog());
  }

  @Autowired
  public DefenseResolver(@Autowired(required = false) GameConfig gameConfig,
                         @Autowired(required = false) com.example.htmlmud.domain.repository.TemplateReader templateReader) {
    this.gameConfig = gameConfig;
    this.templateReader = templateReader != null ? templateReader : new com.example.htmlmud.domain.service.TemplateCatalog();
  }

  /**
   * 解算 MUD 端生靈之間的單次攻擊檢定 (隨機擲骰)
   */
  public DefenseResolution resolveLivingAttack(com.example.htmlmud.domain.actor.impl.Living attacker,
                                               com.example.htmlmud.domain.actor.impl.Living defender,
                                               int rawDamage) {
    return resolveLivingAttack(attacker, defender, rawDamage, null);
  }

  /**
   * 解算 MUD 端生靈之間的單次攻擊檢定 (支援指定骰值便於單元測試驗證)
   */
  public DefenseResolution resolveLivingAttack(com.example.htmlmud.domain.actor.impl.Living attacker,
                                               com.example.htmlmud.domain.actor.impl.Living defender,
                                               int rawDamage,
                                               Double predeterminedRoll) {
    String attackerName = (attacker != null) ? attacker.getName() : "攻擊者";
    String defenderName = (defender != null) ? defender.getName() : "防禦者";
    String weaponName = (attacker != null && attacker.getMainHandWeapon() != null)
        ? attacker.getMainHandWeapon().getDisplayName()
        : "兵刃";

    int atkDex = (attacker != null && attacker.getStats() != null) ? attacker.getStats().getDex() : 10;
    int defDex = (defender != null && defender.getStats() != null) ? defender.getStats().getDex() : 5;
    int defStr = (defender != null && defender.getStats() != null) ? defender.getStats().getStr() : 5;
    int defCon = (defender != null && defender.getStats() != null) ? defender.getStats().getCon() : 5;

    SkillTemplate dodgeSkill = null;
    SkillTemplate parrySkill = null;
    SkillTemplate forceSkill = null;

    if (defender != null && templateReader != null) {
      String dodgeId = defender.getEnabledSkillId(SkillCategory.DODGE);
      if (dodgeId != null) dodgeSkill = templateReader.findSkill(dodgeId).orElse(null);

      String parryId = defender.getEnabledSkillId(SkillCategory.PARRY);
      if (parryId != null) parrySkill = templateReader.findSkill(parryId).orElse(null);

      String forceId = defender.getEnabledSkillId(SkillCategory.FORCE);
      if (forceId != null) forceSkill = templateReader.findSkill(forceId).orElse(null);
    }

    com.example.htmlmud.domain.model.entity.GameItem offHand = (defender != null) ? defender.getOffHandEquip() : null;
    boolean hasShield = (offHand != null && (offHand.getType() == com.example.htmlmud.domain.model.enums.ItemType.SHIELD
        || "SHIELD".equalsIgnoreCase(offHand.getSubType())));
    int shieldBonusDef = 0;
    String shieldName = "護身盾";
    if (hasShield) {
      shieldName = offHand.getDisplayName();
      if (offHand.getTemplate() != null && offHand.getTemplate().equipmentProp() != null) {
        shieldBonusDef = offHand.getTemplate().equipmentProp().defense();
      } else if (offHand.getDefinition() != null) {
        shieldBonusDef = offHand.getDefinition().bonusDefense();
      }
    }

    String defWeapon = (defender != null && defender.getMainHandWeapon() != null)
        ? defender.getMainHandWeapon().getDisplayName()
        : "兵刃";

    CombatResourceType resType = (defender != null && defender.getStats() != null) ? CombatResourceType.SP : CombatResourceType.SP;

    boolean canRiposte = false;
    int riposteAtk = 5;
    if (defender instanceof com.example.htmlmud.domain.actor.impl.Player p) {
      canRiposte = "SWORDSMAN".equalsIgnoreCase(p.getClassId()) || parrySkill != null;
      if (p.getStats() != null) {
        riposteAtk = Math.max(5, (int)(p.getStats().getStr() * 1.2 + p.getStats().getDex()));
      }
    } else if (defender != null) {
      canRiposte = parrySkill != null;
      riposteAtk = Math.max(5, (defender.minDamage + defender.maxDamage) / 2);
    }

    return executeCombatTable(attackerName, defenderName, weaponName, defWeapon,
        atkDex, defDex, defStr, defCon,
        dodgeSkill, parrySkill, forceSkill,
        hasShield, shieldBonusDef, shieldName,
        rawDamage, null, predeterminedRoll, null, resType,
        canRiposte, riposteAtk,
        (defender != null ? defender.getStats() : null));
  }

  /**
   * 解算敵怪對小隊成員的單次攻擊檢定 (隨機擲骰)
   */
  public DefenseResolution resolveEnemyAttack(BattleEnemy enemy, PartyMember targetMember, int rawDamage, String enemyMove) {
    return resolveEnemyAttack(enemy, targetMember, rawDamage, enemyMove, null);
  }

  /**
   * 解算敵怪對小隊成員的單次攻擊檢定 (支援指定骰值便於單元測試驗證)
   */
  public DefenseResolution resolveEnemyAttack(BattleEnemy enemy, PartyMember targetMember, int rawDamage, String enemyMove, Double predeterminedRoll) {
    String attackerName = (enemy != null) ? enemy.getName() : "敵人";
    String defenderName = (targetMember != null) ? targetMember.getName() : "防禦者";
    String weaponName = "利爪牙刃";

    int defDex = (targetMember != null && targetMember.getStats() != null) ? targetMember.getStats().getDex() : 5;
    int defStr = (targetMember != null && targetMember.getStats() != null) ? targetMember.getStats().getStr() : 5;
    int defCon = (targetMember != null && targetMember.getStats() != null) ? targetMember.getStats().getCon() : 5;
    int atkDex = (enemy != null) ? enemy.getDex() : 10;

    SkillTemplate dodgeSkill = (targetMember != null) ? targetMember.getEnabledPassive(SkillCategory.DODGE) : null;
    if (dodgeSkill == null && targetMember != null && templateReader != null) {
      String sId = targetMember.getEnabledPassiveSkillId(SkillCategory.DODGE);
      if (sId != null) dodgeSkill = templateReader.findSkill(sId).orElse(null);
    }

    SkillTemplate parrySkill = (targetMember != null) ? targetMember.getEnabledPassive(SkillCategory.PARRY) : null;
    if (parrySkill == null && targetMember != null && templateReader != null) {
      String sId = targetMember.getEnabledPassiveSkillId(SkillCategory.PARRY);
      if (sId != null) parrySkill = templateReader.findSkill(sId).orElse(null);
    }

    SkillTemplate forceSkill = (targetMember != null) ? targetMember.getEnabledPassive(SkillCategory.FORCE) : null;
    if (forceSkill == null && targetMember != null && templateReader != null) {
      String sId = targetMember.getEnabledPassiveSkillId(SkillCategory.FORCE);
      if (sId != null) forceSkill = templateReader.findSkill(sId).orElse(null);
    }

    PartyItemSlot shield = (targetMember != null) ? targetMember.getEquippedShield() : null;
    boolean hasShield = (shield != null && shield.isShield());
    int shieldBonusDef = (shield != null) ? shield.getBonusDefense() : 0;
    String shieldName = (shield != null && shield.getName() != null && !shield.getName().isBlank()) ? shield.getName() : "護身盾";

    String defWeapon = (targetMember != null && targetMember.getEquippedWeapon() != null)
        ? targetMember.getEquippedWeapon().getName()
        : "兵刃";

    CombatResourceType resType = (targetMember != null) ? targetMember.getResourceType() : CombatResourceType.SP;

    boolean canRiposte = false;
    int riposteAtk = 5;
    if (targetMember != null) {
      canRiposte = "SWORDSMAN".equalsIgnoreCase(targetMember.getClassId()) || parrySkill != null;
      riposteAtk = Math.max(5, targetMember.getEffectiveMaxDamage());
    }

    return executeCombatTable(attackerName, defenderName, weaponName, defWeapon,
        atkDex, defDex, defStr, defCon,
        dodgeSkill, parrySkill, forceSkill,
        hasShield, shieldBonusDef, shieldName,
        rawDamage, enemyMove, predeterminedRoll, targetMember, resType,
        canRiposte, riposteAtk,
        (targetMember != null ? targetMember.getStats() : null));
  }

  private DefenseResolution executeCombatTable(
      String attackerName, String defenderName, String weaponName, String defWeapon,
      int atkDex, int defDex, int defStr, int defCon,
      SkillTemplate dodgeSkill, SkillTemplate parrySkill, SkillTemplate forceSkill,
      boolean hasShield, int shieldBonusDef, String shieldName,
      int rawDamage, String enemyMove, Double predeterminedRoll,
      PartyMember targetMember, CombatResourceType resType) {
    return executeCombatTable(attackerName, defenderName, weaponName, defWeapon,
        atkDex, defDex, defStr, defCon,
        dodgeSkill, parrySkill, forceSkill,
        hasShield, shieldBonusDef, shieldName,
        rawDamage, enemyMove, predeterminedRoll, targetMember, resType,
        false, 0, (targetMember != null ? targetMember.getStats() : null));
  }

  private DefenseResolution executeCombatTable(
      String attackerName, String defenderName, String weaponName, String defWeapon,
      int atkDex, int defDex, int defStr, int defCon,
      SkillTemplate dodgeSkill, SkillTemplate parrySkill, SkillTemplate forceSkill,
      boolean hasShield, int shieldBonusDef, String shieldName,
      int rawDamage, String enemyMove, Double predeterminedRoll,
      PartyMember targetMember, CombatResourceType resType,
      boolean canRiposte, int riposteAttackPower) {
    return executeCombatTable(attackerName, defenderName, weaponName, defWeapon,
        atkDex, defDex, defStr, defCon,
        dodgeSkill, parrySkill, forceSkill,
        hasShield, shieldBonusDef, shieldName,
        rawDamage, enemyMove, predeterminedRoll, targetMember, resType,
        canRiposte, riposteAttackPower, (targetMember != null ? targetMember.getStats() : null));
  }

  private DefenseResolution executeCombatTable(
      String attackerName, String defenderName, String weaponName, String defWeapon,
      int atkDex, int defDex, int defStr, int defCon,
      SkillTemplate dodgeSkill, SkillTemplate parrySkill, SkillTemplate forceSkill,
      boolean hasShield, int shieldBonusDef, String shieldName,
      int rawDamage, String enemyMove, Double predeterminedRoll,
      PartyMember targetMember, CombatResourceType resType,
      boolean canRiposte, int riposteAttackPower,
      com.example.htmlmud.domain.model.entity.LivingStats defStats) {

    // 檢查防禦者精力狀態 (Stamina / Poise Break)
    int currentStamina = (defStats != null) ? defStats.getStamina() : 100;
    boolean poiseBroken = (currentStamina <= 0);

    // 1. Miss 閾值 (機率)：BaseMiss(5%) + max(0, (Def.DEX - Atk.DEX) * 0.5%)
    double missChance = Math.min(0.25, Math.max(0.02, 0.05 + Math.max(0, defDex - atkDex) * 0.005));

    // 2. Dodge 閾值 (機率)：Skill.dodgeRate + (Def.DEX * 0.8%) - (Atk.DEX * 0.3%)
    // 若處於精力枯竭狀態 (Stamina <= 0)，閃避機率強制歸零
    double dodgeChance = 0.0;
    if (!poiseBroken && dodgeSkill != null) {
      double baseDodge = 0.10;
      if (dodgeSkill.getMechanics() != null) {
        if (dodgeSkill.getMechanics().dodgeRate() > 0) {
          baseDodge = dodgeSkill.getMechanics().dodgeRate();
        } else if (dodgeSkill.getMechanics().dodgeMod() > 0) {
          baseDodge = dodgeSkill.getMechanics().dodgeMod();
        }
      }
      dodgeChance = Math.min(0.75, Math.max(0.05, baseDodge + (defDex * 0.008) - (atkDex * 0.003)));
    }

    // 3. Parry 閾值 (機率)：Skill.parryRate + (Def.STR * 0.4%) + (Def.DEX * 0.4%)
    // 若處於精力枯竭狀態 (Stamina <= 0)，招架機率強制歸零
    double parryChance = 0.0;
    double reduceRatio = 0.50; // 招架傷害承受比率 (預設減免 50%)
    if (!poiseBroken && parrySkill != null) {
      double baseParry = 0.15;
      if (parrySkill != null && parrySkill.getMechanics() != null) {
        if (parrySkill.getMechanics().parryRate() > 0) {
          baseParry = parrySkill.getMechanics().parryRate();
        } else if (parrySkill.getMechanics().parryMod() > 0) {
          baseParry = parrySkill.getMechanics().parryMod();
        }
        if (parrySkill.getMechanics().damageReduce() > 0) {
          double extraReduce = parrySkill.getMechanics().damageReduce();
          reduceRatio = Math.max(0.20, 0.50 - extraReduce);
        }
      }
      parryChance = Math.min(0.65, Math.max(0.05, baseParry + (defStr * 0.004) + (defDex * 0.004)));
    }

    // 4. Block 閾值 (機率)：副手配備盾牌時參與圓桌判定 (基礎 20% + CON * 0.3%)
    double blockChance = 0.0;
    if (hasShield) {
      blockChance = Math.min(0.60, Math.max(0.10, 0.20 + (defCon * 0.003)));
    }

    // 5. Crit 閾值 (機率)：攻擊者暴擊率 (基礎 5% + Atk.DEX * 0.2%)
    double critChance = Math.min(0.35, Math.max(0.05, 0.05 + (atkDex * 0.002)));

    // =========================================================================
    // 圓桌總機率安全收斂與歸一化 (Probability Normalization & Safe Allocation)
    // 1. 防禦切片總和 (Miss + Dodge + Parry + Block) 不可突破全域上限 (maxTotalDef, 預設 0.75)
    // 2. 致命一擊 (Crit) 僅分配剩餘機率，且保留至少 minNormalHit (預設 0.05) 作為普通命中窗口
    // =========================================================================
    double maxTotalDef = (gameConfig != null && gameConfig.getCombat() != null)
        ? gameConfig.getCombat().getMaxTotalDefenseChance()
        : 0.75;
    double minNormalHit = (gameConfig != null && gameConfig.getCombat() != null)
        ? gameConfig.getCombat().getMinNormalHitChance()
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

    // =========================================================================
    // 一元一次擲骰圓桌判定 (One-Roll Resolution)
    // 累積邊界: [0, Miss) -> [Miss, Dodge) -> [Dodge, Parry) -> [Parry, Block) -> [Block, Crit) -> [Crit, 1.0)
    // =========================================================================
    double roll = (predeterminedRoll != null)
        ? predeterminedRoll
        : ThreadLocalRandom.current().nextDouble();

    double missLimit = missChance;
    double dodgeLimit = missLimit + dodgeChance;
    double parryLimit = dodgeLimit + parryChance;
    double blockLimit = parryLimit + blockChance;
    double critLimit = blockLimit + actualCrit;

    // 內功護體微調 (FORCE Mitigation)
    int forceReduction = 0;
    if (forceSkill != null && forceSkill.getMechanics() != null && forceSkill.getMechanics().defenseMod() > 0) {
      forceReduction = forceSkill.getMechanics().defenseMod();
    }

    // 判定 1: 未命中 (MISS)
    if (roll < missLimit) {
      String missLog = "\u001B[1;30m💨【未命中】" + attackerName + " 攻勢落空，未能觸及 " + defenderName + "！\u001B[0m";
      return new DefenseResolution(DefenseOutcome.MISS, 0, 0, 0, missLog, false, 0, 0, poiseBroken);
    }

    // 判定 2: 身法閃避 (DODGED) - 消耗 2 點 Stamina
    if (roll < dodgeLimit) {
      int staminaCost = 2;
      if (defStats != null) {
        defStats.setStamina(Math.max(0, defStats.getStamina() - staminaCost));
      }
      String msg = extractDodgeMessage(dodgeSkill, attackerName, defenderName, weaponName);
      String log = "\u001B[1;36m💨【身法閃避】" + msg + "\u001B[0m";
      return new DefenseResolution(DefenseOutcome.DODGED, 0, 15, 0, log, false, 0, staminaCost, false);
    }

    // 判定 3: 招架格擋 (PARRIED) - 消耗 3 點 Stamina
    if (roll < parryLimit) {
      int staminaCost = 3;
      if (defStats != null) {
        defStats.setStamina(Math.max(0, defStats.getStamina() - staminaCost));
      }
      int parriedDmg = Math.max(1, (int) Math.round(rawDamage * reduceRatio));
      String msg = extractParryMessage(parrySkill, attackerName, defenderName, weaponName, defWeapon);
      String log = "\u001B[1;33m🛡️【招架格擋】" + msg + "（傷害減免至 " + parriedDmg + " 點）\u001B[0m";

      boolean triggeredRiposte = false;
      int riposteDmg = 0;
      if (canRiposte) {
        boolean shouldTrigger = (predeterminedRoll != null) || (ThreadLocalRandom.current().nextDouble() < 0.50);
        if (shouldTrigger) {
          triggeredRiposte = true;
          riposteDmg = Math.max(3, riposteAttackPower);
          log += "\r\n\u001B[1;36m⚔️【破招反擊】" + defenderName + " 借力打力，反手一記突刺，對 " + attackerName + " 造成了 " + riposteDmg + " 點反擊傷害！\u001B[0m";
        }
      }

      return new DefenseResolution(DefenseOutcome.PARRIED, parriedDmg, 5, 5, log, triggeredRiposte, riposteDmg, staminaCost, false);
    }

    // 判定 4: 盾牌格擋 (BLOCKED)
    if (roll < blockLimit) {
      int shieldBlockValue = Math.max(5, (shieldBonusDef * 2) + (defCon / 2));
      int blockedDmg = Math.max(1, rawDamage - shieldBlockValue);
      if (poiseBroken) {
        blockedDmg = Math.max(1, (int) Math.round(blockedDmg * 1.20));
      }
      String log = "\u001B[1;33m🛡️【盾牌格擋】" + defenderName + " 舉起【" + shieldName + "】固若金湯，化解了 " + shieldBlockValue + " 點衝擊！（承受 " + blockedDmg + " 點傷害）\u001B[0m";
      if (poiseBroken) {
        log += "\r\n\u001B[1;35m⚠️【架勢破防】" + defenderName + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
      }
      return new DefenseResolution(DefenseOutcome.BLOCKED, blockedDmg, 5, 5, log, false, 0, 0, poiseBroken);
    }

    // 判定 5: 致命一擊 (CRIT)
    if (roll < critLimit) {
      int critDmg = Math.max(1, (int) Math.round(rawDamage * 1.5) - forceReduction);
      if (poiseBroken) {
        critDmg = Math.max(1, (int) Math.round(critDmg * 1.20));
      }
      String critLog = "\u001B[1;31m💥【致命一擊】" + attackerName + " 破開空隙正中要害，對 " + defenderName + " 爆擊造成 " + critDmg + " 點毀滅傷害！\u001B[0m";
      if (poiseBroken) {
        critLog += "\r\n\u001B[1;35m⚠️【架勢破防】" + defenderName + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
      }
      return new DefenseResolution(DefenseOutcome.CRIT, critDmg, 10, 15, critLog, false, 0, 0, poiseBroken);
    }

    // 判定 6: 普通命中受創 (HIT)
    int finalDmg = Math.max(1, rawDamage - forceReduction);
    if (poiseBroken) {
      finalDmg = Math.max(1, (int) Math.round(finalDmg * 1.20));
    }
    int rage = (resType == CombatResourceType.RAGE) ? 15 : 0;
    String hitLog;
    if (enemyMove != null && !enemyMove.isBlank()) {
      hitLog = "\u001B[1;31m⚡【" + attackerName + "】施展【" + enemyMove + "】，重創 " + defenderName + " 造成 " + finalDmg + " 點傷害！\u001B[0m";
    } else {
      hitLog = "\u001B[1;31m⚡【" + attackerName + "】發起猛烈撲擊，重創 " + defenderName + " 造成 " + finalDmg + " 點傷害！\u001B[0m";
    }
    if (poiseBroken) {
      hitLog += "\r\n\u001B[1;35m⚠️【架勢破防】" + defenderName + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
    }

    return new DefenseResolution(DefenseOutcome.HIT, finalDmg, 10, rage, hitLog, false, 0, 0, poiseBroken);
  }

  /**
   * 資料驅動抽取 DODGE 文本
   */
  private String extractDodgeMessage(SkillTemplate skill, String attacker, String defender, String weapon) {
    if (skill != null) {
      // 1. 優先嘗試 SkillTemplate.messages.dodge
      if (skill.getMessages() != null && skill.getMessages().containsKey("dodge")) {
        Object dodgeObj = skill.getMessages().get("dodge");
        if (dodgeObj instanceof List<?> list && !list.isEmpty()) {
          int idx = ThreadLocalRandom.current().nextInt(list.size());
          return replacePlaceholders(String.valueOf(list.get(idx)), attacker, defender, weapon, null);
        } else if (dodgeObj instanceof String str && !str.isBlank()) {
          return replacePlaceholders(str, attacker, defender, weapon, null);
        }
      }
      // 2. 次嘗試 moves 裡的 msg.success
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

  /**
   * 資料驅動抽取 PARRY 文本
   */
  private String extractParryMessage(SkillTemplate skill, String attacker, String defender, String weapon, String defWeapon) {
    if (defWeapon == null || defWeapon.isBlank()) {
      defWeapon = "兵刃";
    }

    if (skill != null) {
      // 1. 優先嘗試 SkillTemplate.messages.parried
      if (skill.getMessages() != null && skill.getMessages().containsKey("parried")) {
        Object parriedObj = skill.getMessages().get("parried");
        if (parriedObj instanceof List<?> list && !list.isEmpty()) {
          int idx = ThreadLocalRandom.current().nextInt(list.size());
          return replacePlaceholders(String.valueOf(list.get(idx)), attacker, defender, weapon, defWeapon);
        } else if (parriedObj instanceof String str && !str.isBlank()) {
          return replacePlaceholders(str, attacker, defender, weapon, defWeapon);
        }
      }
      // 2. 次嘗試 moves 裡的 msg.success
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

  /**
   * 替換 MUD 經典佔位符:
   * $N: 攻擊者 (Attacker)
   * $n: 防守者 (Defender)
   * $w: 攻擊者兵器 (Attacker weapon)
   * $W: 防守者兵器 (Defender weapon)
   * $l: 部位 (預設身側)
   */
  private String replacePlaceholders(String template, String attacker, String defender, String weapon, String defWeapon) {
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
