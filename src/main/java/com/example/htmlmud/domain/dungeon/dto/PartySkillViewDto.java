package com.example.htmlmud.domain.dungeon.dto;

public record PartySkillViewDto(
    String id,
    String name,
    String icon,
    String description,
    String costType,
    int costValue,
    String costDescription,
    long cooldownMs,
    long remainingCooldownMs,
    boolean available,
    String category,
    boolean synergy
) {
  public PartySkillViewDto(
      String id,
      String name,
      String icon,
      String description,
      String costType,
      int costValue,
      String costDescription,
      long cooldownMs,
      long remainingCooldownMs,
      boolean available
  ) {
    this(id, name, icon, description, costType, costValue, costDescription, cooldownMs, remainingCooldownMs, available, "CLASS", false);
  }
}
