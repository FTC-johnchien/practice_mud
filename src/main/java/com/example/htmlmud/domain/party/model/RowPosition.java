package com.example.htmlmud.domain.party.model;

import lombok.Getter;

@Getter
public enum RowPosition {
  FRONT("前衛", 0.70, 1.0), // 承擔 70% 近戰受擊率，近戰威力 100%
  MIDDLE("中衛", 0.50, 0.90), // 承擔 50% 受擊率，近戰威力 90%
  BACK("後衛", 0.30, 0.75), // 承擔 30% 受擊率（受前衛掩護），近戰威力 75%（需遠程/道術）
  ANY("任意", 0.50, 1.0);

  private final String chineseName;
  private final double aggroWeight;
  private final double meleeEfficiency;

  RowPosition(String chineseName, double aggroWeight, double meleeEfficiency) {
    this.chineseName = chineseName;
    this.aggroWeight = aggroWeight;
    this.meleeEfficiency = meleeEfficiency;
  }
}
