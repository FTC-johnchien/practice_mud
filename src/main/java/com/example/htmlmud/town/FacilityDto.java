package com.example.htmlmud.town;

import java.util.List;
import lombok.Builder;

/**
 * 營地機能設施前端視圖投影 DTO
 */
@Builder
public record FacilityDto(
    String id,
    String name,
    String facilityType,
    String icon,
    String description,
    String npcId,
    String npcName,
    List<String> targetDungeons,
    List<String> actions,
    boolean isOpen,
    boolean glitch
) {}
