package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleContext {
  private String battleId;
  private String playerId;
  private Party party;
  @Builder.Default
  private List<BattleEnemy> enemies = new ArrayList<>();
  @Builder.Default
  private BattleState state = BattleState.STARTING;
  @Builder.Default
  private int selectedTargetIndex = 0;
  private String tauntedByMemberId;
  @Builder.Default
  private long tauntedUntil = 0;
  @Builder.Default
  private ConcurrentLinkedQueue<String> battleLogs = new ConcurrentLinkedQueue<>();
  @Builder.Default
  private long startTime = System.currentTimeMillis();
  @Builder.Default
  private long lastHeartbeat = System.currentTimeMillis();

  public boolean isOver() {
    return state == BattleState.VICTORY || state == BattleState.DEFEAT || state == BattleState.FLED;
  }

  public boolean isAllEnemiesDead() {
    return enemies.stream().noneMatch(BattleEnemy::isAlive);
  }

  public boolean isAllPartyDead() {
    return party.getMembers().stream().noneMatch(PartyMember::isAlive);
  }

  public BattleEnemy getTargetEnemy() {
    if (selectedTargetIndex >= 0 && selectedTargetIndex < enemies.size()) {
      BattleEnemy enemy = enemies.get(selectedTargetIndex);
      if (enemy.isAlive()) {
        return enemy;
      }
    }
    // 自動回退至第一個活著的敵人
    for (int i = 0; i < enemies.size(); i++) {
      if (enemies.get(i).isAlive()) {
        selectedTargetIndex = i;
        return enemies.get(i);
      }
    }
    return null;
  }

  public BattleEnemy getFrontTargetEnemy() {
    // 優先找前排活著的
    for (int i = 0; i < enemies.size(); i++) {
      BattleEnemy e = enemies.get(i);
      if (e.isAlive() && e.getRow() == RowPosition.FRONT) {
        return e;
      }
    }
    // 前排無人生還，轉向後排
    return getTargetEnemy();
  }

  public void addLog(String log) {
    battleLogs.add(log);
    while (battleLogs.size() > 25) {
      battleLogs.poll();
    }
  }

  public List<String> getRecentLogs() {
    return new ArrayList<>(battleLogs);
  }

  public boolean isTaunted() {
    return tauntedByMemberId != null && tauntedUntil > System.currentTimeMillis();
  }

  public void setTaunt(String memberId, long durationMs) {
    this.tauntedByMemberId = memberId;
    this.tauntedUntil = System.currentTimeMillis() + durationMs;
  }
}
