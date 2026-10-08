package com.example.htmlmud.domain.dungeon.dto;

import java.util.List;

/**
 * 戰鬥大捷結構化結算 DTO (BATTLE_VICTORY 事件載荷)
 * 供前端渲染大捷翻牌視窗、經驗條充能與突破光環
 */
public record BattleVictoryDto(
    String type,
    String battleId,
    int totalXp,
    int coinsGained,
    List<MemberLevelUpDto> levelUps,
    List<BattleLootDto> loots
) {
  public BattleVictoryDto(String battleId, int totalXp, int coinsGained,
                          List<MemberLevelUpDto> levelUps, List<BattleLootDto> loots) {
    this("BATTLE_VICTORY", battleId, totalXp, coinsGained,
         levelUps != null ? levelUps : List.of(),
         loots != null ? loots : List.of());
  }

  public record MemberLevelUpDto(
      String memberId,
      String memberName,
      int oldLevel,
      int newLevel,
      String statGrowthDesc
  ) {}

  public record BattleLootDto(
      String itemId,
      String name,
      String icon,
      String quality,
      int count,
      String description
  ) {}
}
