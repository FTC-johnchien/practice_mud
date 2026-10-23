package com.example.htmlmud.town;

import java.util.List;
import com.example.htmlmud.town.PerceptionTier;
import lombok.Builder;

/**
 * 營地機能中樞聚合視圖 DTO (Town Hub DTO)
 * 對齊 Level 2《M1_TOWN_HUB_AND_FACILITIES.md》
 */
@Builder
public record TownHubDto(
    String zoneId,
    String zoneName,
    String description,
    int chipRate,
    PerceptionTier perceptionTier,
    List<FacilityDto> facilities
) {}
