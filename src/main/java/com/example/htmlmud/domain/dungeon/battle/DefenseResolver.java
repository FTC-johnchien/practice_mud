package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.htmlmud.config.GameConfig;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.template.model.SkillTemplate;
import com.example.htmlmud.party.CombatResourceType;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyMember;

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
  private final com.example.htmlmud.template.TemplateReader templateReader;

  public DefenseResolver() {
    this(null, new com.example.htmlmud.template.TemplateCatalog());
  }

  @Autowired
  public DefenseResolver(@Autowired(required = false) GameConfig gameConfig,
                         @Autowired(required = false) com.example.htmlmud.template.TemplateReader templateReader) {
    this.gameConfig = gameConfig;
    this.templateReader = templateReader != null ? templateReader : new com.example.htmlmud.template.TemplateCatalog();
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
      boolean classCanRiposte = p.getClassTemplate().map(t -> t.hasTrait("canRiposte")).orElse(false);
      canRiposte = classCanRiposte || parrySkill != null;
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
      boolean classCanRiposte = targetMember.getClassTemplate().map(t -> t.hasTrait("canRiposte")).orElse(false);
      canRiposte = classCanRiposte || parrySkill != null;
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

    com.example.htmlmud.domain.combat.CombatRules.DefenseParams params =
        new com.example.htmlmud.domain.combat.CombatRules.DefenseParams(
            attackerName, defenderName, weaponName, defWeapon,
            atkDex, defDex, defStr, defCon,
            dodgeSkill, parrySkill, forceSkill,
            hasShield, shieldBonusDef, shieldName,
            rawDamage, enemyMove, resType,
            canRiposte, riposteAttackPower, defStats,
            this.gameConfig
        );
    return com.example.htmlmud.domain.combat.CombatRules.executeCombatTable(params, predeterminedRoll);
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
