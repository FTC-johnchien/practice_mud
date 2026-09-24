package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.PartyMember;

/**
 * 戰鬥防禦與被動心法檢定解算器 (Generic Defense & Passive Resolver)
 * 負責小隊成員在遭受攻擊時的閃避 (DODGE)、招架 (PARRY)、內功減免 (FORCE) 檢定與日誌生成。
 * 遵循「機制運行 + 資料驅動」原則：絕不寫死技能 ID，所有係數與文本全從 SkillTemplate 抽取。
 */
@Component
public class DefenseResolver {

  public enum DefenseOutcome {
    HIT,      // 普通命中受創
    DODGED,   // 身法閃避成功 (0傷害, +15 SP)
    PARRIED   // 招架格擋成功 (傷害減免 50%~70%, +5 SP)
  }

  public record DefenseResolution(
      DefenseOutcome outcome,
      int finalDamage,
      int spGained,
      int rageGained,
      String combatLog
  ) {}

  /**
   * 解算敵怪對小隊成員的單次攻擊檢定
   *
   * @param enemy 攻擊者怪物
   * @param targetMember 防禦者隊員
   * @param rawDamage 未經被動檢定的基礎扣防傷害 (min 1)
   * @param enemyMove 敵方招式名稱 (可為 null)
   * @return DefenseResolution 結算結果
   */
  public DefenseResolution resolveEnemyAttack(BattleEnemy enemy, PartyMember targetMember, int rawDamage, String enemyMove) {
    String attackerName = (enemy != null) ? enemy.getName() : "敵人";
    String defenderName = (targetMember != null) ? targetMember.getName() : "防禦者";
    String weaponName = "利爪牙刃";

    int dex = (targetMember != null && targetMember.getStats() != null) ? targetMember.getStats().getDex() : 5;
    int str = (targetMember != null && targetMember.getStats() != null) ? targetMember.getStats().getStr() : 5;
    int con = (targetMember != null && targetMember.getStats() != null) ? targetMember.getStats().getCon() : 5;

    // =========================================================================
    // 1. 身法閃避檢定 (DODGE Check)
    // =========================================================================
    SkillTemplate dodgeSkill = (targetMember != null) ? targetMember.getEnabledPassive(SkillCategory.DODGE) : null;
    if (dodgeSkill != null) {
      double baseDodge = 0.10; // 預設基礎 10%
      if (dodgeSkill.getMechanics() != null) {
        if (dodgeSkill.getMechanics().dodgeRate() > 0) {
          baseDodge = dodgeSkill.getMechanics().dodgeRate();
        } else if (dodgeSkill.getMechanics().dodgeMod() > 0) {
          baseDodge = dodgeSkill.getMechanics().dodgeMod();
        }
      }
      // DEX 屬性每點增加 0.5% 閃避率
      double dexBonus = (dex * 0.005);
      double totalDodge = Math.min(0.75, Math.max(0.05, baseDodge + dexBonus));

      if (ThreadLocalRandom.current().nextDouble() < totalDodge) {
        String msg = extractDodgeMessage(dodgeSkill, attackerName, defenderName, weaponName);
        String log = "\u001B[1;36m💨【身法閃避】" + msg + "\u001B[0m";
        return new DefenseResolution(DefenseOutcome.DODGED, 0, 15, 0, log);
      }
    }

    // =========================================================================
    // 2. 招架格擋檢定 (PARRY Check)
    // =========================================================================
    SkillTemplate parrySkill = (targetMember != null) ? targetMember.getEnabledPassive(SkillCategory.PARRY) : null;
    if (parrySkill != null) {
      double baseParry = 0.15; // 預設基礎 15%
      double reduceRatio = 0.50; // 預設招架減免 50%
      if (parrySkill.getMechanics() != null) {
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
      // STR 與 CON 均值每點增加 0.4% 招架率
      double statBonus = (((str + con) / 2.0) * 0.004);
      double totalParry = Math.min(0.65, Math.max(0.05, baseParry + statBonus));

      if (ThreadLocalRandom.current().nextDouble() < totalParry) {
        int parriedDmg = Math.max(1, (int) Math.round(rawDamage * reduceRatio));
        String msg = extractParryMessage(parrySkill, attackerName, defenderName, weaponName, targetMember);
        String log = "\u001B[1;33m🛡️【招架格擋】" + msg + "（傷害減免至 " + parriedDmg + " 點）\u001B[0m";
        return new DefenseResolution(DefenseOutcome.PARRIED, parriedDmg, 5, 5, log);
      }
    }

    // =========================================================================
    // 3. 內功護體微調 (FORCE Mitigation)
    // =========================================================================
    int finalDmg = rawDamage;
    SkillTemplate forceSkill = (targetMember != null) ? targetMember.getEnabledPassive(SkillCategory.FORCE) : null;
    if (forceSkill != null && forceSkill.getMechanics() != null) {
      if (forceSkill.getMechanics().defenseMod() > 0) {
        finalDmg = Math.max(1, finalDmg - forceSkill.getMechanics().defenseMod());
      }
    }

    // =========================================================================
    // 4. 普通命中受創 (HIT)
    // =========================================================================
    int rage = (targetMember != null && targetMember.getResourceType() == CombatResourceType.RAGE) ? 15 : 0;
    String hitLog;
    if (enemyMove != null && !enemyMove.isBlank()) {
      hitLog = "\u001B[1;31m⚡【" + attackerName + "】施展【" + enemyMove + "】，重創 " + defenderName + " 造成 " + finalDmg + " 點傷害！\u001B[0m";
    } else {
      hitLog = "\u001B[1;31m⚡【" + attackerName + "】發起猛烈撲擊，重創 " + defenderName + " 造成 " + finalDmg + " 點傷害！\u001B[0m";
    }

    return new DefenseResolution(DefenseOutcome.HIT, finalDmg, 10, rage, hitLog);
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
  private String extractParryMessage(SkillTemplate skill, String attacker, String defender, String weapon, PartyMember member) {
    String defWeapon = (member != null && member.getEquippedWeapon() != null)
        ? member.getEquippedWeapon().getName()
        : "兵刃";

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
