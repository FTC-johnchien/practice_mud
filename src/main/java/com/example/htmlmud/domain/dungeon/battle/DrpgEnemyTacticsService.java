package com.example.htmlmud.domain.dungeon.battle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.model.TacticsRule;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.example.htmlmud.infra.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 DRPG 敵方 AI 目標鎖定、仇恨計算、招式動詞生成與隊友戰術 (Gambit) 條件求值。
 */
@Slf4j
@Service
public class DrpgEnemyTacticsService {

  private final TemplateReader templateReader;

  @Autowired
  public DrpgEnemyTacticsService(TemplateReader templateReader) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
  }

  public DrpgEnemyTacticsService() {
    this(new TemplateCatalog());
  }

  /**
   * 根據嘲諷、有效仇恨 (前排 1.3 倍加成) 及站位，智慧選取怪物攻擊目標
   */
  public PartyMember selectPartyTarget(BattleContext ctx) {
    return selectPartyTarget(ctx, null);
  }

  /**
   * 根據個別敵人的獨立威脅表 (Threat Table)、嘲諷狀態與前排權重，選取並同步當前攻擊目標
   */
  public PartyMember selectPartyTarget(BattleContext ctx, BattleEnemy enemy) {
    if (ctx == null || ctx.getParty() == null || ctx.getParty().getMembers() == null) {
      return null;
    }

    // 1. 若該敵人被嘲諷，或戰場全局被嘲諷且嘲諷者存活，強制打嘲諷者
    if (enemy != null && enemy.isTaunted()) {
      for (PartyMember m : ctx.getParty().getMembers()) {
        if (m.getId().equals(enemy.getTauntedByMemberId()) && m.isAlive()) {
          enemy.setTargetMemberId(m.getId());
          enemy.setTargetMemberName(m.getName());
          return m;
        }
      }
    } else if (ctx.isTaunted()) {
      for (PartyMember m : ctx.getParty().getMembers()) {
        if (m.getId().equals(ctx.getTauntedByMemberId()) && m.isAlive()) {
          if (enemy != null) {
            enemy.setTargetMemberId(m.getId());
            enemy.setTargetMemberName(m.getName());
          }
          return m;
        }
      }
    }

    // 2. 存活隊員列表
    List<PartyMember> aliveMembers = ctx.getParty().getMembers().stream()
        .filter(PartyMember::isAlive)
        .toList();
    if (aliveMembers.isEmpty()) {
      return null;
    }

    // 依據有效仇恨 (Effective Threat = Threat * (FRONT ? 1.3 : 1.0)) 評估最高仇恨者
    PartyMember highestThreatMember = null;
    double maxEffectiveThreat = -1;
    for (PartyMember m : aliveMembers) {
      int baseThreat = (enemy != null && enemy.getThreatTable().containsKey(m.getId()))
          ? enemy.getThreat(m.getId())
          : m.getThreat();

      double effectiveThreat = baseThreat * (m.getRow() == RowPosition.FRONT ? 1.3 : 1.0);
      if (effectiveThreat > maxEffectiveThreat) {
        maxEffectiveThreat = effectiveThreat;
        highestThreatMember = m;
      }
    }

    PartyMember chosen = null;
    // 若已有建立仇恨 (maxEffectiveThreat > 0)，直接鎖定最高仇恨者
    if (maxEffectiveThreat > 0 && highestThreatMember != null) {
      chosen = highestThreatMember;
    } else {
      // 3. 初始無仇恨狀態，優先挑選前排活著的隊員
      List<PartyMember> frontAlive = aliveMembers.stream()
          .filter(m -> m.getRow() == RowPosition.FRONT)
          .toList();
      if (!frontAlive.isEmpty()) {
        chosen = frontAlive.get(ThreadLocalRandom.current().nextInt(frontAlive.size()));
      } else {
        chosen = aliveMembers.get(ThreadLocalRandom.current().nextInt(aliveMembers.size()));
      }
    }

    if (enemy != null && chosen != null) {
      enemy.setTargetMemberId(chosen.getId());
      enemy.setTargetMemberName(chosen.getName());
      // 建立基礎交戰仇恨，避免目標完全無仇恨記錄
      if (enemy.getThreat(chosen.getId()) == 0) {
        enemy.addThreat(chosen.getId(), 1);
      }
    }

    return chosen;
  }

  /**
   * 隨機或權重選取怪物施展之種族天生招式或技能名稱
   */
  public String selectEnemyMove(BattleEnemy enemy) {
    if (enemy == null) return null;

    // 1. 先檢驗種族天生招式
    String raceId = enemy.getRace();
    if (raceId != null) {
      var raceOpt = templateReader.findRace(raceId);
      if (raceOpt.isPresent() && raceOpt.get().combat() != null
          && raceOpt.get().combat().naturalAttacks() != null
          && !raceOpt.get().combat().naturalAttacks().isEmpty()) {
        var attack = RandomUtil.pickWeighted(raceOpt.get().combat().naturalAttacks());
        if (attack != null) {
          var skOpt = templateReader.findSkill(attack.getId());
          if (skOpt.isPresent() && skOpt.get().getMoves() != null && !skOpt.get().getMoves().isEmpty()) {
            var moves = skOpt.get().getMoves();
            return moves.get(ThreadLocalRandom.current().nextInt(moves.size())).name();
          }
        }
      }
    }

    // 2. 檢驗怪物自訂技能清單
    if (enemy.getSkills() != null && !enemy.getSkills().isEmpty()) {
      String skId = enemy.getSkills().get(ThreadLocalRandom.current().nextInt(enemy.getSkills().size()));
      var skOpt = templateReader.findSkill(skId);
      if (skOpt.isPresent() && skOpt.get().getMoves() != null && !skOpt.get().getMoves().isEmpty()) {
        var moves = skOpt.get().getMoves();
        return moves.get(ThreadLocalRandom.current().nextInt(moves.size())).name();
      }
    }

    return null;
  }

  /**
   * 計算怪物對隊員傷害
   */
  public int calculateEnemyDamage(BattleEnemy enemy, PartyMember targetMember) {
    int rawDmg = ThreadLocalRandom.current().nextInt(enemy.getMinDamage(), enemy.getMaxDamage() + 1);
    return Math.max(1, rawDmg - targetMember.getEffectiveDefense());
  }

  /**
   * 計算小隊成員對怪物的基礎普通攻擊傷害
   */
  public int calculatePlayerDamage(PartyMember member, BattleEnemy target) {
    int base = ThreadLocalRandom.current().nextInt(member.getBaseMinDamage(), member.getBaseMaxDamage() + 1);
    int net = Math.max(1, base - target.getDefense());
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    return (int) (net * variance);
  }

  /**
   * 隊友戰術方針條件求值 (Gambit System)
   */
  public boolean evaluateTacticsCondition(BattleContext ctx, PartyMember member, TacticsRule rule) {
    if (rule == null || rule.getCondition() == null) return false;

    return switch (rule.getCondition()) {
      case ALLY_HP_LESS_THAN -> ctx.getParty().getMembers().stream()
          .filter(PartyMember::isAlive)
          .anyMatch(m -> m.getStats() != null && m.getStats().getMaxHp() > 0
              && ((double) m.getStats().getHp() / m.getStats().getMaxHp() * 100.0 <= rule.getConditionValue()));

      case SELF_HP_LESS_THAN -> member.getStats() != null && member.getStats().getMaxHp() > 0
          && ((double) member.getStats().getHp() / member.getStats().getMaxHp() * 100.0 <= rule.getConditionValue());

      case ENEMY_COUNT_GTE -> ctx.getEnemies().stream().filter(BattleEnemy::isAlive).count() >= rule.getConditionValue();

      case ENEMY_IS_BOSS -> ctx.getEnemies().stream().anyMatch(e -> e.isAlive()
          && (e.getHp() > 400 || (e.getTemplateId() != null && e.getTemplateId().contains("boss"))));

      case RESOURCE_GTE -> {
        if (member.getResourceType() == CombatResourceType.SP) {
          yield member.getCurrentSp() >= rule.getConditionValue();
        } else if (member.getResourceType() == CombatResourceType.RAGE) {
          yield member.getCurrentRage() >= rule.getConditionValue();
        } else if (member.getResourceType() == CombatResourceType.COMBO) {
          yield member.getCurrentCombo() >= rule.getConditionValue();
        } else if (member.getResourceType() == CombatResourceType.MP && member.getStats() != null) {
          yield member.getStats().getMp() >= rule.getConditionValue();
        }
        yield true;
      }

      case TARGET_LACKS_BUFF -> {
        PartyMember targetAlly = resolveAllyTarget(ctx, member, rule.getTarget(), -1);
        String bId = rule.getSkillId();
        yield targetAlly != null && !targetAlly.hasActiveBuff(bId);
      }

      case TARGET_HAS_BUFF -> {
        PartyMember targetAlly = resolveAllyTarget(ctx, member, rule.getTarget(), -1);
        String bId = rule.getSkillId();
        yield targetAlly != null && targetAlly.hasActiveBuff(bId);
      }

      case BUFF_TIME_LESS_THAN -> {
        PartyMember targetAlly = resolveAllyTarget(ctx, member, rule.getTarget(), -1);
        String bId = rule.getSkillId();
        ActiveBuff ab = (targetAlly != null) ? targetAlly.getActiveBuff(bId) : null;
        yield ab != null && ab.getRemainingSeconds() <= rule.getConditionValue();
      }

      case ALWAYS -> true;
    };
  }

  /**
   * 檢查隊員是否具備足夠資源與冷卻施展技能
   */
  public boolean canCastSkill(PartyMember member, PartyMemberSkill skill) {
    if (member == null || skill == null) return false;
    if (!member.isSkillUsable(skill)) return false;
    if (member.isOnCooldown(skill.getId())) return false;
    if (skill.getCostType() == CombatResourceType.MP) {
      return member.getStats() != null && member.getStats().getMp() >= skill.getCostValue();
    } else if (skill.getCostType() == CombatResourceType.SP) {
      return member.getCurrentSp() >= skill.getCostValue();
    } else if (skill.getCostType() == CombatResourceType.RAGE) {
      return member.getCurrentRage() >= skill.getCostValue();
    } else if (skill.getCostType() == CombatResourceType.COMBO) {
      return member.getCurrentCombo() >= skill.getCostValue();
    } else if (skill.getCostType() == CombatResourceType.HP) {
      return member.getStats() != null && member.getStats().getHp() > skill.getCostValue();
    }
    return true;
  }

  /**
   * 扣除隊員施法資源
   */
  public boolean consumeSkillResource(PartyMember member, PartyMemberSkill skill) {
    if (skill.getCostType() == CombatResourceType.MP) {
      return member.consumeMp(skill.getCostValue());
    } else if (skill.getCostType() == CombatResourceType.SP) {
      return member.consumeSp(skill.getCostValue());
    } else if (skill.getCostType() == CombatResourceType.RAGE) {
      return member.consumeRage(skill.getCostValue());
    } else if (skill.getCostType() == CombatResourceType.COMBO) {
      return member.consumeCombo(skill.getCostValue());
    } else if (skill.getCostType() == CombatResourceType.HP) {
      if (member.getStats() != null && member.getStats().getHp() > skill.getCostValue()) {
        member.takeDamage(skill.getCostValue());
        return true;
      }
      return false;
    }
    return true;
  }

  /**
   * 根據戰術方針或手動指定，解析小隊中的友方目標 (支援 Tank、隊長、指定成員、血量最低隊友、自身等)
   */
  public PartyMember resolveAllyTarget(BattleContext ctx, PartyMember caster, com.example.htmlmud.domain.party.model.TacticsTarget target, int manualTargetIdx) {
    if (ctx == null || ctx.getParty() == null || ctx.getParty().getMembers() == null) {
      return caster;
    }
    List<PartyMember> members = ctx.getParty().getMembers();

    // 1. 若手動提供了有效的隊員下標
    if (manualTargetIdx >= 0 && manualTargetIdx < members.size() && members.get(manualTargetIdx).isAlive()) {
      return members.get(manualTargetIdx);
    }

    if (target == null) {
      target = com.example.htmlmud.domain.party.model.TacticsTarget.LOWEST_HP_ALLY;
    }

    return switch (target) {
      case SELF -> caster;

      case LEADER -> (!members.isEmpty() && members.get(0).isAlive()) ? members.get(0) : caster;

      case FRONT_ROW_ALLY -> {
        // 優先挑選存活的前衛肉盾隊友 (以血量上限與有效防禦最高者為肉盾)
        var frontTank = members.stream()
            .filter(PartyMember::isAlive)
            .filter(m -> m.getRow() == RowPosition.FRONT)
            .max(java.util.Comparator.comparingInt((PartyMember m) -> m.getStats() != null ? m.getStats().getMaxHp() : 0)
                .thenComparingInt(PartyMember::getEffectiveDefense));
        if (frontTank.isPresent()) {
          yield frontTank.get();
        }
        // 若無前衛，退化至全隊血量/防禦最高者
        yield members.stream()
            .filter(PartyMember::isAlive)
            .max(java.util.Comparator.comparingInt((PartyMember m) -> m.getStats() != null ? m.getStats().getMaxHp() : 0))
            .orElse(caster);
      }

      case BACK_ROW_ALLY -> members.stream()
          .filter(PartyMember::isAlive)
          .filter(m -> m.getRow() == RowPosition.BACK)
          .findFirst()
          .orElse(caster);

      case MEMBER_1 -> (members.size() > 0 && members.get(0).isAlive()) ? members.get(0) : caster;
      case MEMBER_2 -> (members.size() > 1 && members.get(1).isAlive()) ? members.get(1) : caster;
      case MEMBER_3 -> (members.size() > 2 && members.get(2).isAlive()) ? members.get(2) : caster;
      case MEMBER_4 -> (members.size() > 3 && members.get(3).isAlive()) ? members.get(3) : caster;
      case MEMBER_5 -> (members.size() > 4 && members.get(4).isAlive()) ? members.get(4) : caster;

      case LOWEST_HP_ALLY, ALL_ALLIES -> members.stream()
          .filter(PartyMember::isAlive)
          .min((a, b) -> Double.compare(
              (double) a.getStats().getHp() / Math.max(1, a.getStats().getMaxHp()),
              (double) b.getStats().getHp() / Math.max(1, b.getStats().getMaxHp())))
          .orElse(caster);

      default -> caster;
    };
  }
}
