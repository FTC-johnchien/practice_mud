package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.RowPosition;
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
  private ConcurrentLinkedQueue<com.example.htmlmud.domain.dungeon.battle.event.BattleEvent> pendingEvents = new ConcurrentLinkedQueue<>();
  @Builder.Default
  private java.util.concurrent.atomic.AtomicLong eventSeq = new java.util.concurrent.atomic.AtomicLong(0);
  @Builder.Default
  private long startTime = System.currentTimeMillis();
  @Builder.Default
  private long lastActivityTime = System.currentTimeMillis(); // 玩家最後操作時間戳記
  @Builder.Default
  private long maxIdleDurationMs = 600_000L; // 預設 10 分鐘無操作放置超時
  @Builder.Default
  private long maxDurationMs = 300_000L; // 預設 5 分鐘
  @Builder.Default
  private int maxRounds = 50_000; // 原 100 回合放寬為防禦極限安全閾值 (約 7 小時)，常規由 10 分鐘放置超時守護
  @Builder.Default
  private int roundCount = 0;
  private java.util.concurrent.Future<?> combatFuture;
  @Builder.Default
  private long lastHeartbeat = System.currentTimeMillis();
  private java.util.function.Consumer<BattleState> onBattleEnd;

  // === F3-01: 古仙陣法動態連攜隊列與共鳴度 (Cross Combos & Formation Resonance) ===
  @Builder.Default
  private int crossComboStep = 0;             // 當前連攜階數 (0: 未啟動, 1: 先導位, 2: 雙星合璧, 3: 三才混元, 4: 四象誅仙)
  private String crossComboLastActorId;       // 上一出招隊員 ID
  private String crossComboLastActorName;     // 上一出招隊員姓名
  private String crossComboTargetId;          // 連攜鎖定目標 ID
  @Builder.Default
  private long crossComboExpiresAt = 0L;      // 連攜時間窗口 (預設出招後 3500ms)
  @Builder.Default
  private int resonancePoints = 0;            // 全隊共享陣法共鳴點數 (0~5 點)

  public void addResonance(int amount) {
    this.resonancePoints = Math.max(0, Math.min(5, this.resonancePoints + amount));
  }

  public void resetCrossCombo() {
    this.crossComboStep = 0;
    this.crossComboLastActorId = null;
    this.crossComboLastActorName = null;
    this.crossComboTargetId = null;
    this.crossComboExpiresAt = 0L;
  }


  public void touchActivity() {
    this.lastActivityTime = System.currentTimeMillis();
  }

  public boolean isIdleTimedOut() {
    return (System.currentTimeMillis() - lastActivityTime) >= maxIdleDurationMs;
  }

  public boolean isOver() {
    return state == BattleState.VICTORY || state == BattleState.DEFEAT || state == BattleState.FLED || state == BattleState.TIMEOUT;
  }

  public boolean isTimedOut() {
    boolean durationExpired = (maxDurationMs > 0 && (System.currentTimeMillis() - startTime) >= maxDurationMs);
    return durationExpired || isIdleTimedOut();
  }

  public boolean isMaxRoundsExceeded() {
    return roundCount >= maxRounds;
  }

  public void cancelBattle() {
    if (combatFuture != null && !combatFuture.isDone()) {
      combatFuture.cancel(true);
    }
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

  public void checkEnemyRowAdvancement() {
    if (enemies == null) return;
    boolean hasLivingFront = enemies.stream().anyMatch(e -> e.isAlive() && e.getRow() == RowPosition.FRONT);
    if (!hasLivingFront) {
      for (BattleEnemy e : enemies) {
        if (e.isAlive() && e.getRow() == RowPosition.BACK) {
          e.setRow(RowPosition.FRONT);
        }
      }
    }
  }

  public BattleEnemy getFrontTargetEnemy() {
    checkEnemyRowAdvancement();
    // 1. 若玩家已明確指定鎖定集火目標，且該目標活著，全隊集中火力優先擊殺！
    if (selectedTargetIndex >= 0 && selectedTargetIndex < enemies.size()) {
      BattleEnemy selected = enemies.get(selectedTargetIndex);
      if (selected.isAlive()) {
        return selected;
      }
    }
    // 2. 否則前衛自動尋敵：優先找前排活著的
    for (int i = 0; i < enemies.size(); i++) {
      BattleEnemy e = enemies.get(i);
      if (e.isAlive() && e.getRow() == RowPosition.FRONT) {
        selectedTargetIndex = i;
        return e;
      }
    }
    // 3. 前排無人生還，轉向任意存活後排敵人
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

  public void emit(com.example.htmlmud.domain.dungeon.battle.event.BattleEvent event) {
    if (event == null) return;
    pendingEvents.add(event);
    while (pendingEvents.size() > 200) {
      pendingEvents.poll();
    }
  }

  public long nextEventSeq() {
    return eventSeq.incrementAndGet();
  }

  public List<com.example.htmlmud.domain.dungeon.battle.event.BattleEvent> drainEvents() {
    List<com.example.htmlmud.domain.dungeon.battle.event.BattleEvent> list = new ArrayList<>();
    com.example.htmlmud.domain.dungeon.battle.event.BattleEvent e;
    while ((e = pendingEvents.poll()) != null) {
      list.add(e);
    }
    return list;
  }

  public void setTaunt(String memberId, long durationMs) {
    this.tauntedByMemberId = memberId;
    this.tauntedUntil = System.currentTimeMillis() + durationMs;
  }
}
