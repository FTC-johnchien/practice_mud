package com.example.htmlmud.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/**
 * 認知透鏡條目 (Cognitive Perception Lens)
 * 供營地設施、裝備物品、戰技與對話結構化掛載，依據晶片加載率進行多階動態投影
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CognitiveLens(
    int minProgress,
    String name,
    @JsonAlias({"description", "text", "lore"})
    String desc,
    boolean glitch
) {}
