package com.example.htmlmud.template.model;

import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestTemplate(
    String id,
    String title,
    String triggerMobId,
    String logMessage,
    List<String> storyLines,
    List<String> rewardItemIds,
    String unlockExitRoomId
) {
  public QuestTemplate {
    if (storyLines == null) storyLines = Collections.emptyList();
    if (rewardItemIds == null) rewardItemIds = Collections.emptyList();
  }
}
