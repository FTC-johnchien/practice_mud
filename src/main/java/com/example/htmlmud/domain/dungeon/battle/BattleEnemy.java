package com.example.htmlmud.domain.dungeon.battle;

import com.example.htmlmud.domain.party.model.RowPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleEnemy {
  private String id;
  private String templateId;
  private String name;
  private int hp;
  private int maxHp;
  @Builder.Default
  private int minDamage = 12;
  @Builder.Default
  private int maxDamage = 20;
  @Builder.Default
  private int defense = 5;
  @Builder.Default
  private int dex = 10;
  @Builder.Default
  private RowPosition row = RowPosition.FRONT;
  @Builder.Default
  private long attackIntervalMs = 2200;
  @Builder.Default
  private long nextAttackTime = 0;
  @Builder.Default
  private long stunnedUntil = 0;
  @Builder.Default
  private boolean alive = true;
  @Builder.Default
  private int xp = 30;
  private String dropItemId;
  private String droppedDaoSkillId;
  private String droppedDaoSkillName;
  private String droppedDaoMemberName;

  public void takeDamage(int dmg) {
    this.hp = Math.max(0, this.hp - dmg);
    if (this.hp <= 0) {
      this.alive = false;
    }
  }

  public boolean isStunned() {
    return stunnedUntil > System.currentTimeMillis();
  }

  public void applyStun(long durationMs) {
    this.stunnedUntil = Math.max(this.stunnedUntil, System.currentTimeMillis() + durationMs);
  }
}
