package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.CombatResourceType;

/**
 * 小隊多人合擊技能解算器 (Party Combo / Synergy Resolver)
 * 負責檢定多角色存活狀態、排除失控狀態、檢驗複合資源並執行合擊殺傷與增益
 */
@Component
public class ComboResolver {

  /**
   * 檢查指定合擊技能是否可被施展
   */
  public boolean canExecuteCombo(BattleContext ctx, PartyMemberSkill skill) {
    if (ctx == null || ctx.isOver() || skill == null) return false;
    Party party = ctx.getParty();
    if (party == null) return false;

    // 1. 存活人數檢定
    long aliveCount = party.getMembers().stream().filter(PartyMember::isAlive).count();
    if (aliveCount < skill.getMinPartyAlive()) return false;

    // 2. 陣法要求檢定
    if (skill.getRequiredFormation() != null && !skill.getRequiredFormation().isBlank()) {
      if (party.getEquippedFormation() == null ||
          !skill.getRequiredFormation().equalsIgnoreCase(party.getEquippedFormation().getId())) {
        return false;
      }
    }

    // 3. 陣法靈威檢定
    if (skill.getFormationEnergyCost() > 0 && party.getFormationEnergy() < skill.getFormationEnergyCost()) {
      return false;
    }

    // 4. 參與職業成員與狀態檢定
    List<PartyMember> participants = findParticipants(party, skill);
    if (participants == null) return false;

    // 5. 參與者資源檢定
    int spCost = skill.getSpCost() > 0 ? skill.getSpCost() : (skill.getCostType() == CombatResourceType.SP || skill.getCostType() == CombatResourceType.RAGE ? skill.getCostValue() : 0);
    int mpCost = skill.getMpCost() > 0 ? skill.getMpCost() : (skill.getCostType() == CombatResourceType.MP ? skill.getCostValue() : 0);

    for (PartyMember p : participants) {
      if (spCost > 0 && p.getCurrentSp() < spCost) return false;
      if (mpCost > 0 && (p.getStats() == null || p.getStats().getMp() < mpCost)) return false;
    }

    return true;
  }

  /**
   * 尋找符合合擊技能需求且健康的隊員清單
   */
  public List<PartyMember> findParticipants(Party party, PartyMemberSkill skill) {
    if (party == null || skill == null) return null;
    List<String> reqClasses = skill.getRequiredClasses();
    if (reqClasses == null || reqClasses.isEmpty()) {
      // 若無限定職業，取前 minPartyAlive 位健康隊員
      List<PartyMember> valid = party.getMembers().stream()
          .filter(this::isHealthyCombatant)
          .limit(Math.max(1, skill.getMinPartyAlive()))
          .toList();
      return valid.size() >= skill.getMinPartyAlive() ? new ArrayList<>(valid) : null;
    }

    List<PartyMember> participants = new ArrayList<>();
    Set<String> matchedMemberIds = new HashSet<>();

    for (String reqClass : reqClasses) {
      PartyMember match = party.getMembers().stream()
          .filter(m -> !matchedMemberIds.contains(m.getId()))
          .filter(this::isHealthyCombatant)
          .filter(m -> m.getClassId() != null && m.getClassId().equalsIgnoreCase(reqClass))
          .findFirst()
          .orElse(null);

      if (match == null) {
        return null; // 缺少特定職業成員
      }
      matchedMemberIds.add(match.getId());
      participants.add(match);
    }

    return participants;
  }

  /**
   * 判斷隊員是否具備出招能力（存活且未陷入定身、走火入魔或畸變）
   */
  public boolean isHealthyCombatant(PartyMember m) {
    if (m == null || !m.isAlive()) return false;
    PartyMember.MadnessState st = m.getMadnessState();
    return st != PartyMember.MadnessState.SEALED
        && st != PartyMember.MadnessState.CHAOS
        && st != PartyMember.MadnessState.ABERRATION
        && st != PartyMember.MadnessState.DEAD_MEAT;
  }

  /**
   * 執行合擊技能
   */
  public boolean executeCombo(Player player, BattleContext ctx, PartyMemberSkill skill, java.util.function.BiConsumer<Player, String> logBroadcaster) {
    if (!canExecuteCombo(ctx, skill)) {
      return false;
    }

    Party party = ctx.getParty();
    List<PartyMember> participants = findParticipants(party, skill);
    if (participants == null || participants.isEmpty()) return false;

    // 1. 扣除陣法靈威
    if (skill.getFormationEnergyCost() > 0) {
      party.addFormationEnergy(-skill.getFormationEnergyCost());
    }

    // 2. 扣除全體參與者資源
    int spCost = skill.getSpCost() > 0 ? skill.getSpCost() : (skill.getCostType() == CombatResourceType.SP || skill.getCostType() == CombatResourceType.RAGE ? skill.getCostValue() : 0);
    int mpCost = skill.getMpCost() > 0 ? skill.getMpCost() : (skill.getCostType() == CombatResourceType.MP ? skill.getCostValue() : 0);

    for (PartyMember p : participants) {
      if (spCost > 0) p.consumeSp(spCost);
      if (mpCost > 0) p.consumeMp(mpCost);
    }

    // 3. 計算威力與效果
    double totalAtk = participants.stream().mapToInt(PartyMember::getEffectiveMaxDamage).sum();
    int baseDmg = (int) (totalAtk * skill.getDamageMultiplier());

    List<String> names = participants.stream().map(PartyMember::getName).toList();
    String participantNames = String.join(" 與 ", names);

    // 4. 傷害結算
    if (skill.isHeal()) {
      int healAmt = Math.max(80, (int) (skill.getHealAmount() > 0 ? skill.getHealAmount() : totalAtk * 1.5));
      for (PartyMember m : party.getMembers()) {
        if (m.isAlive()) {
          m.heal(healAmt);
          if (skill.getSanRestore() > 0) {
            m.restoreSan(skill.getSanRestore());
          }
        }
      }
      logBroadcaster.accept(player, "\u001B[1;36m🌟🌟【多人合擊】" + participantNames + " 攜手引動【" + skill.getName() + "】！神光普照，全隊恢復 " + healAmt + " 氣血與 " + skill.getSanRestore() + " 點道心！\u001B[0m");
    } else {
      if (skill.isAoe()) {
        for (BattleEnemy e : ctx.getEnemies()) {
          if (e.isAlive()) {
            e.takeDamage(baseDmg);
            if (skill.isStun()) {
              e.applyStun(skill.getStunDurationSeconds() > 0 ? skill.getStunDurationSeconds() * 1000L : 4000L);
            }
          }
        }
        logBroadcaster.accept(player, "\u001B[1;33m⚔️⚔️【小隊合擊】" + participantNames + " 氣機相融，聯手爆發【" + skill.getName() + "】！全體敵怪遭受 " + baseDmg + " 點毀滅打擊！\u001B[0m");
      } else {
        BattleEnemy target = ctx.getTargetEnemy();
        if (target != null && target.isAlive()) {
          target.takeDamage(baseDmg);
          if (skill.isStun()) {
            target.applyStun(skill.getStunDurationSeconds() > 0 ? skill.getStunDurationSeconds() * 1000L : 4000L);
          }
          logBroadcaster.accept(player, "\u001B[1;33m⚔️⚔️【小隊合擊】" + participantNames + " 凌空合擊，聯手發動【" + skill.getName() + "】！直貫【" + target.getName() + "】造成 " + baseDmg + " 點貫穿巨創！\u001B[0m");
        }
      }
    }

    if (ctx.isAllEnemiesDead()) {
      ctx.setState(BattleState.VICTORY);
    }

    return true;
  }
}
