package com.example.htmlmud.domain.party.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 隊員專屬主動戰鬥技能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMemberSkill {
  private String id;
  private String name;
  private String icon;
  private String description;
  private ResourceType costType;
  private int costValue;
  private long cooldownMs;
  @Builder.Default
  private double damageMultiplier = 1.0;
  @Builder.Default
  private boolean aoe = false;
  @Builder.Default
  private boolean heal = false;
  @Builder.Default
  private boolean taunt = false;
  @Builder.Default
  private boolean stun = false;
  @Builder.Default
  private int stunDurationSeconds = 0;
  @Builder.Default
  private int healAmount = 0;
  @Builder.Default
  private int sanRestore = 0;
}
