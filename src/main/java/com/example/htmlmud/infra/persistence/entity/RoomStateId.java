package com.example.htmlmud.infra.persistence.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomStateId implements Serializable {
  private String roomId;
  private String zoneId;
}
