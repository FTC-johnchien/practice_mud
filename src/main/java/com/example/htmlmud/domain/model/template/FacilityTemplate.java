package com.example.htmlmud.domain.model.template;

import java.util.List;
import com.example.htmlmud.domain.model.vo.CognitiveLens;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/**
 * 營地機能設施靜態範本 (Facility Template)
 * 對應 data/zones/{zoneId}/facilities.json 定義
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FacilityTemplate(
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
    List<CognitiveLens> lenses
) {}
