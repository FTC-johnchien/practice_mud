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
}
