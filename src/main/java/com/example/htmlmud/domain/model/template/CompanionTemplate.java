package com.example.htmlmud.domain.model.template;

import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.Gender;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import lombok.Builder;

@Builder(toBuilder = true)
public record CompanionTemplate(
    String id,
    String name,
    String roleTitle,
    String classId,
    Gender gender,
    RowPosition defaultRow,
    ResourceType resourceType,
    int maxHp,
    int maxMp,
    int maxSan,
    int baseMinDamage,
    int baseMaxDamage,
    int baseDefense,
    int str,
    int con,
    int dex,
    int intelligence,
    Map<EquipmentSlot, String> initialEquipment,
    List<String> skills,
    List<String> aliases,
    String race,
    String homeRoomId,
    String description,
    String lookDescription,
    List<String> dialogues,
    List<String> learnedStances,
    List<NpcCapability> capabilities
) {
  public CompanionTemplate {
    if (gender == null) gender = Gender.MALE;
    if (defaultRow == null) defaultRow = RowPosition.FRONT;
    if (resourceType == null) resourceType = ResourceType.MP;
    if (initialEquipment == null) initialEquipment = Map.of();
    if (skills == null) skills = List.of();
    if (aliases == null) aliases = List.of();
    if (dialogues == null) dialogues = List.of();
    if (learnedStances == null) learnedStances = List.of();
    if (capabilities == null) capabilities = List.of();
  }

  public MobTemplate toMobTemplate() {
    return toMobTemplate(null);
  }

  public MobTemplate toMobTemplate(String zoneId) {
    String effectiveId = (zoneId != null && !zoneId.isBlank()) ? zoneId + ":" + id : id;
    Map<String, String> eq = new java.util.HashMap<>();
    if (initialEquipment != null) {
      for (var entry : initialEquipment.entrySet()) {
        eq.put(entry.getKey().name(), entry.getValue());
      }
    }
    return MobTemplate.builder()
        .id(effectiveId)
        .name(name)
        .race(race != null ? race : "human")
        .gender(gender != null ? gender : Gender.MALE)
        .aliases(aliases != null && !aliases.isEmpty() ? aliases : List.of(id, name))
        .kind(com.example.htmlmud.domain.model.enums.MobKind.FRIENDLY)
        .rank(com.example.htmlmud.domain.model.enums.MobRank.NORMAL)
        .isUnique(true)
        .level(10)
        .maxHp(maxHp)
        .maxMp(maxMp)
        .maxStamina(100)
        .str(str)
        .con(con)
        .dex(dex)
        .intelligence(intelligence)
        .minDamage(baseMinDamage)
        .maxDamage(baseMaxDamage)
        .defense(baseDefense)
        .description(description != null ? description : roleTitle)
        .lookDescription(lookDescription != null ? lookDescription : (description != null ? description : roleTitle))
        .dialogues(dialogues != null ? dialogues : List.of())
        .equipment(eq)
        .capabilities(capabilities != null ? capabilities : List.of())
        .build();
  }
}
