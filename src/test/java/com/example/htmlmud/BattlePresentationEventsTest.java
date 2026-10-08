package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEvent;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEventType;
import com.example.htmlmud.domain.dungeon.battle.event.BattleHit;
import com.example.htmlmud.domain.dungeon.battle.event.FxShape;
import com.example.htmlmud.domain.dungeon.battle.event.HitOutcome;
import com.example.htmlmud.domain.dungeon.battle.event.UnitRef;
import com.example.htmlmud.domain.dungeon.dto.BattleEventsDto;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.model.entity.LivingStats;

class BattlePresentationEventsTest {

  @Test
  @DisplayName("驗證 BattleContext 事件佇列發佈、單調遞增序號與清空 drain 機制")
  void testBattleContextEventLifecycle() {
    BattleContext ctx = BattleContext.builder()
        .battleId("b-test-01")
        .build();

    assertEquals(0, ctx.drainEvents().size());

    long seq1 = ctx.nextEventSeq();
    long seq2 = ctx.nextEventSeq();
    assertEquals(1, seq1);
    assertEquals(2, seq2);

    BattleEvent ev1 = new BattleEvent(
        seq1,
        System.currentTimeMillis(),
        BattleEventType.ATTACK,
        new UnitRef(UnitRef.Side.PARTY, "p-1", 0),
        null,
        "平刺",
        "SWORD",
        "PHYSICAL",
        FxShape.SINGLE,
        null,
        List.of(new BattleHit(new UnitRef(UnitRef.Side.ENEMY, "e-1", 0), HitOutcome.HIT, 35, false))
    );

    BattleEvent ev2 = new BattleEvent(
        seq2,
        System.currentTimeMillis(),
        BattleEventType.ATTACK,
        new UnitRef(UnitRef.Side.ENEMY, "e-1", 0),
        null,
        "利爪撲擊",
        "NATURAL",
        "PHYSICAL",
        FxShape.SINGLE,
        null,
        List.of(new BattleHit(new UnitRef(UnitRef.Side.PARTY, "p-1", 0), HitOutcome.DODGED, 0, false))
    );

    ctx.emit(ev1);
    ctx.emit(ev2);

    List<BattleEvent> drained = ctx.drainEvents();
    assertEquals(2, drained.size());
    assertEquals(1, drained.get(0).seq());
    assertEquals(HitOutcome.HIT, drained.get(0).hits().get(0).outcome());
    assertEquals(2, drained.get(1).seq());
    assertEquals(HitOutcome.DODGED, drained.get(1).hits().get(0).outcome());

    // 再次 drain 必須為空，確保不會跨次重播
    assertTrue(ctx.drainEvents().isEmpty());
  }

  @Test
  @DisplayName("驗證 BattleEventsDto 封裝規格與不可變性")
  void testBattleEventsDtoContract() {
    BattleEvent ev = new BattleEvent(
        100L,
        System.currentTimeMillis(),
        BattleEventType.RIPOSTE,
        new UnitRef(UnitRef.Side.PARTY, "hero", 0),
        null,
        "破招反擊",
        "SWORD",
        "PHYSICAL",
        FxShape.SINGLE,
        null,
        List.of(new BattleHit(new UnitRef(UnitRef.Side.ENEMY, "mob", 1), HitOutcome.CRIT, 72, true))
    );

    BattleEventsDto dto = new BattleEventsDto("b-999", List.of(ev));
    assertEquals("BATTLE_EVENTS", dto.type());
    assertEquals("b-999", dto.battleId());
    assertEquals(1, dto.events().size());
    assertEquals(HitOutcome.CRIT, dto.events().get(0).hits().get(0).outcome());
    assertTrue(dto.events().get(0).hits().get(0).killed());
  }

  @Test
  @DisplayName("驗證全體攻擊 (AOE Skill) 正確派發多目標 BattleEvent 與 hits 清單")
  void testAoeSkillEmitsBattleEventWithAllHits() {
    com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService tacticsService =
        new com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService();
    com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop combatLoop =
        new com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop(tacticsService, null, null, null, null, null);

    LivingStats heroStats = new LivingStats();
    heroStats.setMaxHp(200);
    heroStats.setHp(200);

    PartyMember hero = PartyMember.builder()
        .id("hero-1")
        .name("凌霜")
        .stats(heroStats)
        .baseMinDamage(20)
        .baseMaxDamage(30)
        .build();

    Party party = new Party();
    party.getMembers().add(hero);

    BattleEnemy enemy1 = BattleEnemy.builder().id("e-1").name("狼怪甲").hp(50).maxHp(50).alive(true).build();
    BattleEnemy enemy2 = BattleEnemy.builder().id("e-2").name("狼怪乙").hp(60).maxHp(60).alive(true).build();

    BattleContext ctx = BattleContext.builder()
        .battleId("b-aoe-test")
        .party(party)
        .enemies(new java.util.ArrayList<>(List.of(enemy1, enemy2)))
        .build();

    PartyMemberSkill aoeSkill = PartyMemberSkill.builder()
        .id("thunder_aoe")
        .name("九天神雷")
        .aoe(true)
        .damageMultiplier(1.5)
        .tags(List.of("LIGHTNING"))
        .build();

    combatLoop.applySkillEffects(null, ctx, hero, aoeSkill, -1, "");

    List<BattleEvent> events = ctx.drainEvents();
    assertEquals(1, events.size(), "AOE 攻擊應產生 1 個包含多個 hits 的 BattleEvent");
    BattleEvent ev = events.get(0);
    assertEquals(BattleEventType.SKILL, ev.type());
    assertEquals("thunder_aoe", ev.skillId());
    assertEquals("九天神雷", ev.skillName());
    assertEquals("LIGHTNING", ev.damageType());
    assertEquals(FxShape.ALL, ev.shape());
    assertEquals(2, ev.hits().size(), "hits 清單應涵蓋敵方全體 2 個存活目標");
    assertEquals("e-1", ev.hits().get(0).target().id());
    assertEquals("e-2", ev.hits().get(1).target().id());
    assertTrue(ev.hits().get(0).amount() > 0);
    assertTrue(ev.hits().get(1).amount() > 0);
  }
}
