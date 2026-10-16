package com.example.htmlmud.infra.persistence.entity;

import java.util.List;
import com.example.htmlmud.domain.model.entity.GameItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomStateEntity {

  public RoomStateEntity() {}

  private String roomId;
  private String zoneId;
  private List<GameItem> droppedItems;
}
