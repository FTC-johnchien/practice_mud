package com.example.htmlmud.domain.model.template;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NpcCapability(
    String type,
    String label,
    String command,
    String icon,
    String shopId,
    List<String> trainSkills,
    List<String> quests
) {
  public enum NpcCapabilityType {
    TALK, SHOP, REST, RECRUIT, DISMISS, TRAIN, QUEST, FIGHT
  }

  public NpcCapability {
    if (trainSkills == null) trainSkills = List.of();
    if (quests == null) quests = List.of();
  }

  public boolean isType(NpcCapabilityType targetType) {
    return targetType != null && targetType.name().equalsIgnoreCase(type);
  }

  public boolean isType(String targetType) {
    return type != null && type.equalsIgnoreCase(targetType);
  }
}
