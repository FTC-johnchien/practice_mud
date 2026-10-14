package com.example.htmlmud.infra.persistence.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class RoomStateId implements Serializable {
  private String roomId;
  private String zoneId;

  public RoomStateId() {}

  public RoomStateId(String roomId, String zoneId) {
    this.roomId = roomId;
    this.zoneId = zoneId;
  }
}
