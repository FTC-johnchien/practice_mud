package com.example.htmlmud.domain.dungeon.battle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * 戰鬥結算輸出不可變記錄 (Immutable Battle Outcome)
 * 封裝戰鬥勝利/失敗後的所有數值變更、修為獲得、升級成長與掉落道具，
 * 供專屬 Applier 單向套用至 Player Actor，並具備冪等性保證。
 */
@Builder
public record BattleOutcome(
    String battleId,
    String characterId,
    boolean victory,
    int finalHp,
    int finalMp,
    int hpDelta,
    int mpDelta,
    long expGained,
    int oldLevel,
    int newLevel,
    boolean leveledUp,
    int freeStatPointsGained,
    Map<String, Integer> statIncreases,
    List<String> droppedItemIds,
    List<String> announcements,
    String idempotencyKey
) {

  public BattleOutcome {
    if (statIncreases == null) {
      statIncreases = Collections.emptyMap();
    } else {
      statIncreases = Collections.unmodifiableMap(statIncreases);
    }
    if (droppedItemIds == null) {
      droppedItemIds = Collections.emptyList();
    } else {
      droppedItemIds = Collections.unmodifiableList(droppedItemIds);
    }
    if (announcements == null) {
      announcements = Collections.emptyList();
    } else {
      announcements = Collections.unmodifiableList(announcements);
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      idempotencyKey = (battleId != null && !battleId.isBlank()) ? battleId : java.util.UUID.randomUUID().toString();
    }
  }
}
