package com.example.htmlmud.infra.persistence.entity;

import java.time.LocalDateTime;
import java.util.List;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterEntity {

  public CharacterEntity() {
    this.createdAt = LocalDateTime.now();
    this.currentRoomId = "newbie_village:entrance";
  }

  private String id;
  private String uid;
  private String name;
  private String nickname;
  private String lookDescription;
  private String currentRoomId;
  private LivingStats stats;
  private List<GameItem> inventory;
  private LocalDateTime createdAt;
  private LocalDateTime modifyAt;
}
