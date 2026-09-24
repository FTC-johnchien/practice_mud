package com.example.htmlmud.domain.dungeon.dto;

public record BattleEnemyViewDto(
    int index,
    String id,
    String name,
    int hp,
    int maxHp,
    String row,
    boolean alive,
    boolean isTarget,
    boolean isStunned,
    String targetMemberId,
    String targetMemberName,
    int threat
) {
  public BattleEnemyViewDto(
      int index,
      String id,
      String name,
      int hp,
      int maxHp,
      String row,
      boolean alive,
      boolean isTarget,
      boolean isStunned
  ) {
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, null, null, 0);
  }
}
