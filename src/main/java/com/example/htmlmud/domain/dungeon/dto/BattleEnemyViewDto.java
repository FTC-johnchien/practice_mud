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
    int threat,
    boolean isLarge,
    int size,
    String slot,
    int rowIdx,
    int colIdx,
    int width,
    int height,
    String intentIcon,
    String intentName,
    String intentType,
    String intentTargetScope,
    boolean isCasting,
    long castDurationMs,
    long castRemainingMs,
    boolean isInterruptible
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
      boolean isStunned,
      String targetMemberId,
      String targetMemberName,
      int threat,
      boolean isLarge,
      int size,
      String slot,
      int rowIdx,
      int colIdx,
      int width,
      int height
  ) {
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, targetMemberId, targetMemberName, threat, isLarge, size, slot, rowIdx, colIdx, width, height, null, null, "PHYSICAL", "SINGLE", false, 0L, 0L, true);
  }
  public BattleEnemyViewDto(
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
      int threat,
      boolean isLarge,
      int size,
      String slot,
      int rowIdx,
      int colIdx
  ) {
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, targetMemberId, targetMemberName, threat, isLarge, size, slot, rowIdx, colIdx, size, size);
  }
  public BattleEnemyViewDto(
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
      int threat,
      boolean isLarge,
      int size,
      String slot
  ) {
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, targetMemberId, targetMemberName, threat, isLarge, size, slot, 1, 2);
  }
  public BattleEnemyViewDto(
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
      int threat,
      boolean isLarge,
      int size
  ) {
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, targetMemberId, targetMemberName, threat, isLarge, size, "CENTER");
  }

  public BattleEnemyViewDto(
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
      int threat,
      boolean isLarge
  ) {
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, targetMemberId, targetMemberName, threat, isLarge, isLarge ? 2 : 1, "CENTER");
  }

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
    this(index, id, name, hp, maxHp, row, alive, isTarget, isStunned, null, null, 0, false, 1, "CENTER");
  }
}
