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
import com.example.htmlmud.domain.combat.CombatRules;
import com.example.htmlmud.domain.combat.CombatRules.CrossComboResolution;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEvent;

import com.example.htmlmud.domain.dungeon.battle.event.BattleEventType;
import com.example.htmlmud.domain.dungeon.battle.event.BattleHit;
import com.example.htmlmud.domain.dungeon.battle.event.FxShape;
import com.example.htmlmud.domain.dungeon.battle.event.HitOutcome;
import com.example.htmlmud.domain.dungeon.battle.event.UnitRef;

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
    long now = System.currentTimeMillis();
    PartyMember primary = participants.get(0);
    int primaryIdx = party.getMembers().indexOf(primary);
    String weaponType = primary.getMainHandWeaponType() != null ? primary.getMainHandWeaponType().name() : "SWORD";

    if (skill.isHeal()) {
      int healAmt = Math.max(80, (int) (skill.getHealAmount() > 0 ? skill.getHealAmount() : totalAtk * 1.5));
      List<BattleHit> hits = new ArrayList<>();
      for (PartyMember m : party.getMembers()) {
        if (m.isAlive()) {
          m.heal(healAmt);
          if (skill.getSanRestore() > 0) {
            m.restoreSan(skill.getSanRestore());
          }
          int mIdx = party.getMembers().indexOf(m);
          hits.add(new BattleHit(
              new UnitRef(UnitRef.Side.PARTY, m.getId(), mIdx >= 0 ? mIdx : 0),
              HitOutcome.HIT,
              healAmt,
              0,
              false
          ));
        }
      }
      logBroadcaster.accept(player, "\u001B[1;36m🌟🌟【多人合擊】" + participantNames + " 攜手引動【" + skill.getName() + "】！神光普照，全隊恢復 " + healAmt + " 氣血與 " + skill.getSanRestore() + " 點道心！\u001B[0m");
      ctx.emit(new BattleEvent(
          ctx.nextEventSeq(),
          now,
          BattleEventType.HEAL,
          new UnitRef(UnitRef.Side.PARTY, primary.getId(), primaryIdx >= 0 ? primaryIdx : 0),
          skill.getId(),
          skill.getName(),
          weaponType,
          "HOLY",
          FxShape.ALLY_ALL,
          "combo_heal",
          hits
      ));
    } else {
      if (skill.isAoe()) {
        List<BattleHit> hits = new ArrayList<>();
        for (BattleEnemy e : ctx.getEnemies()) {
          if (e.isAlive()) {
            boolean isStaggerCrit = e.isPoiseBroken();
            int dmg = isStaggerCrit ? com.example.htmlmud.domain.combat.CombatRules.applyPoiseBreakCrit(baseDmg, true) : baseDmg;
            e.takeDamage(dmg);
            if (e.isAlive() && e.checkPhaseTransition() > e.getCurrentPhase()) {
              int nextPhase = e.checkPhaseTransition();
              e.transitionToPhase(nextPhase);
              if (e.isEnraged()) {
                logBroadcaster.accept(player, "\u001B[1;31m⚡⚡⚡【超載狂暴・第 " + nextPhase + " 階段】" + e.getName() + " 體內能量核心超載狂暴，罡氣屏障重聚暴漲！\u001B[0m");
              } else {
                logBroadcaster.accept(player, "\u001B[1;33m⚡⚡【轉階段・第 " + nextPhase + " 階段】" + e.getName() + " 受創激怒，體內禁制崩解，重整旗鼓！\u001B[0m");
              }
            }
            if (skill.isStun()) {
              e.applyStun(skill.getStunDurationSeconds() > 0 ? skill.getStunDurationSeconds() * 1000L : 4000L);
            }
            int eIdx = ctx.getEnemies().indexOf(e);
            hits.add(new BattleHit(
                new UnitRef(UnitRef.Side.ENEMY, e.getId(), eIdx >= 0 ? eIdx : 0),
                isStaggerCrit ? HitOutcome.CRIT : HitOutcome.HIT,
                dmg,
                0,
                !e.isAlive()
            ));
          }
        }
        logBroadcaster.accept(player, "\u001B[1;33m⚔️⚔️【小隊合擊】" + participantNames + " 氣機相融，聯手爆發【" + skill.getName() + "】！全體敵怪遭受毀滅打擊！\u001B[0m");
        ctx.emit(new BattleEvent(
            ctx.nextEventSeq(),
            now,
            BattleEventType.SKILL,
            new UnitRef(UnitRef.Side.PARTY, primary.getId(), primaryIdx >= 0 ? primaryIdx : 0),
            skill.getId(),
            skill.getName(),
            weaponType,
            "PHYSICAL",
            FxShape.ALL,
            "combo_aoe",
            hits
        ));
      } else {
        BattleEnemy target = ctx.getTargetEnemy();
        if (target != null && target.isAlive()) {
          boolean isStaggerCrit = target.isPoiseBroken();
          int dmg = isStaggerCrit ? com.example.htmlmud.domain.combat.CombatRules.applyPoiseBreakCrit(baseDmg, true) : baseDmg;
          target.takeDamage(dmg);
          if (target.isAlive() && target.checkPhaseTransition() > target.getCurrentPhase()) {
            int nextPhase = target.checkPhaseTransition();
            target.transitionToPhase(nextPhase);
            if (target.isEnraged()) {
              logBroadcaster.accept(player, "\u001B[1;31m⚡⚡⚡【超載狂暴・第 " + nextPhase + " 階段】" + target.getName() + " 體內能量核心超載狂暴，罡氣屏障重聚暴漲！\u001B[0m");
            } else {
              logBroadcaster.accept(player, "\u001B[1;33m⚡⚡【轉階段・第 " + nextPhase + " 階段】" + target.getName() + " 受創激怒，體內禁制崩解，重整旗鼓！\u001B[0m");
            }
          }
          if (skill.isStun()) {
            target.applyStun(skill.getStunDurationSeconds() > 0 ? skill.getStunDurationSeconds() * 1000L : 4000L);
          }
          int eIdx = ctx.getEnemies().indexOf(target);
          if (isStaggerCrit) {
            logBroadcaster.accept(player, "\u001B[1;31m💥【破防重創!】⚔️⚔️【小隊合擊】" + participantNames + " 凌空合擊，聯手發動【" + skill.getName() + "】！直取力竭要害，對【" + target.getName() + "】造成 " + dmg + " 點致命暴擊巨創！\u001B[0m");
          } else {
            logBroadcaster.accept(player, "\u001B[1;33m⚔️⚔️【小隊合擊】" + participantNames + " 凌空合擊，聯手發動【" + skill.getName() + "】！直貫【" + target.getName() + "】造成 " + dmg + " 點貫穿巨創！\u001B[0m");
          }
          ctx.emit(new BattleEvent(
              ctx.nextEventSeq(),
              now,
              BattleEventType.SKILL,
              new UnitRef(UnitRef.Side.PARTY, primary.getId(), primaryIdx >= 0 ? primaryIdx : 0),
              skill.getId(),
              skill.getName(),
              weaponType,
              "PHYSICAL",
              FxShape.SINGLE,
              "combo_single",
              List.of(new BattleHit(
                  new UnitRef(UnitRef.Side.ENEMY, target.getId(), eIdx >= 0 ? eIdx : 0),
                  isStaggerCrit ? HitOutcome.CRIT : HitOutcome.HIT,
                  dmg,
                  0,
                  !target.isAlive()
              ))
          ));
        }
      }
    }

    if (ctx.isAllEnemiesDead()) {
      ctx.setState(BattleState.VICTORY);
    }

    return true;
  }

  /**
   * 推進並結算古仙陣法跨隊員動態連攜 (F3-01)
   */
  public CrossComboResolution processCrossCombo(
      Player player,
      BattleContext ctx,
      PartyMember actor,
      BattleEnemy target,
      int rawDamage,
      int basePoiseDamage,
      java.util.function.BiConsumer<Player, String> logBroadcaster
  ) {
    if (ctx == null || actor == null || target == null || !target.isAlive()) {
      return CrossComboResolution.none();
    }

    Party party = ctx.getParty();
    boolean hasActiveFormation = (party != null && party.getEquippedFormation() != null && !party.isFormationBroken());
    long now = System.currentTimeMillis();

    CrossComboResolution res = CombatRules.evaluateCrossComboStep(
        hasActiveFormation,
        ctx.getCrossComboStep(),
        ctx.getCrossComboLastActorId(),
        actor.getId(),
        actor.getName(),
        target.getId(),
        ctx.getCrossComboTargetId(),
        ctx.getCrossComboExpiresAt(),
        now,
        rawDamage,
        basePoiseDamage
    );

    if (res.chainAdvanced()) {
      ctx.setCrossComboStep(res.newStep());
      ctx.setCrossComboLastActorId(actor.getId());
      ctx.setCrossComboLastActorName(actor.getName());
      ctx.setCrossComboTargetId(target.getId());
      ctx.setCrossComboExpiresAt(now + 3500L); // 3.5 秒連攜窗口
      ctx.addResonance(1);

      if (res.sanRestored() > 0 && party != null) {
        for (PartyMember m : party.getMembers()) {
          if (m.isAlive()) {
            m.restoreSan(res.sanRestored());
          }
        }
      }

      if (logBroadcaster != null && res.logMessage() != null) {
        logBroadcaster.accept(player, "   ↳ " + res.logMessage());
      }

      if (res.finisherReady() && logBroadcaster != null) {
        logBroadcaster.accept(player, "\u001B[1;36m✨✨【陣眼共鳴・萬法歸宗】陣法共鳴達最高層，陣眼修士引動天地真元，【古仙陣法・天外合擊終結絕技】全面就緒！\u001B[0m");
      }
    }

    return res;
  }

  /**
   * 釋放古仙陣法天外合擊終結絕技 (Formation Grand Finisher, F3-01)
   * 需消耗陣法共鳴點數，由陣眼隊員引動全隊靈威爆發滅世天威。
   */
  public boolean executeFormationFinisher(
      Player player,
      BattleContext ctx,
      PartyMember invoker,
      java.util.function.BiConsumer<Player, String> logBroadcaster
  ) {
    if (ctx == null || ctx.isOver() || invoker == null || !invoker.isAlive()) {
      return false;
    }

    Party party = ctx.getParty();
    if (party == null || party.getEquippedFormation() == null || party.isFormationBroken()) {
      if (logBroadcaster != null) {
        logBroadcaster.accept(player, "\u001B[1;31m⚠️ 當前未裝備陣法或陣法已潰散，無法施展天外終結合擊！\u001B[0m");
      }
      return false;
    }

    int resonance = ctx.getResonancePoints();
    if (resonance < 3) {
      if (logBroadcaster != null) {
        logBroadcaster.accept(player, "\u001B[1;33m⚠️ 陣法共鳴度不足（需至少 3 點，當前 " + resonance + " 點），無法召喚天外合擊！\u001B[0m");
      }
      return false;
    }

    // 重置共鳴度與連攜隊列
    ctx.setResonancePoints(0);
    ctx.resetCrossCombo();

    // 匯聚全隊存活弟子攻擊力
    List<PartyMember> aliveMembers = party.getAliveMembers();
    int totalAtk = aliveMembers.stream().mapToInt(PartyMember::getEffectiveMaxDamage).sum();
    double multiplier = 2.5 + (resonance * 0.5); // 3 點 = 4.0x, 4 點 = 4.5x, 5 點 = 5.0x
    int finisherDamage = (int) Math.round(totalAtk * multiplier);
    int poiseDmg = 50 + (resonance * 10);
    int sanHeal = 15;
    int hpHeal = 50;

    // 全隊滋養回穩
    for (PartyMember m : aliveMembers) {
      m.heal(hpHeal);
      m.restoreSan(sanHeal);
    }

    long now = System.currentTimeMillis();
    int invokerIdx = party.getMembers().indexOf(invoker);
    List<BattleHit> hits = new ArrayList<>();

    for (BattleEnemy e : ctx.getEnemies()) {
      if (e.isAlive()) {
        boolean isStaggerCrit = e.isPoiseBroken();
        int finalDmg = isStaggerCrit ? CombatRules.applyPoiseBreakCrit(finisherDamage, true) : finisherDamage;
        e.takeDamage(finalDmg);
        e.applyPoiseDamage(poiseDmg);

        if (e.isAlive() && e.checkPhaseTransition() > e.getCurrentPhase()) {
          int nextPhase = e.checkPhaseTransition();
          e.transitionToPhase(nextPhase);
        }

        int eIdx = ctx.getEnemies().indexOf(e);
        hits.add(new BattleHit(
            new UnitRef(UnitRef.Side.ENEMY, e.getId(), eIdx >= 0 ? eIdx : 0),
            isStaggerCrit ? HitOutcome.CRIT : HitOutcome.HIT,
            finalDmg,
            0,
            !e.isAlive()
        ));
      }
    }

    String formationName = party.getEquippedFormation().getName();
    if (logBroadcaster != null) {
      logBroadcaster.accept(player, "\n\u001B[1;35m⚡⚡🌌【古仙陣法・天外合擊】陣眼 " + invoker.getName()
          + " 踏動陣盤，引動《" + formationName + "》至高天道奧義！全隊共鳴威能破界傾瀉！\u001B[0m");
      logBroadcaster.accept(player, "\u001B[1;36m   ↳ 萬千星煞天外墜落，對敵方全員造成 " + finisherDamage
          + " 點天外毀滅打擊，削韌 " + poiseDmg + " 點！全體隊員沐浴靈光，回復 " + hpHeal + " 氣血與 " + sanHeal + " 點道心！\u001B[0m\n");
    }

    ctx.emit(new BattleEvent(
        ctx.nextEventSeq(),
        now,
        BattleEventType.SKILL,
        new UnitRef(UnitRef.Side.PARTY, invoker.getId(), invokerIdx >= 0 ? invokerIdx : 0),
        "combo_formation_finisher",
        "【天外合擊】" + formationName,
        "FORMATION",
        "HOLY",
        FxShape.ALL,
        "formation_finisher",
        hits
    ));

    if (ctx.isAllEnemiesDead()) {
      ctx.setState(BattleState.VICTORY);
    }

    return true;
  }
}

