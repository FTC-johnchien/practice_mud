package com.example.htmlmud.infra.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.htmlmud.domain.model.json.LivingState;
import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class RoomEntity {
  @Id
  public Long id;
  public Long worldId;
  public Long areaId;
  public String name;
  public String description;

  // @JdbcTypeCode(SqlTypes.JSON)
  // @Column(name = "exits_json")
  // public RoomExitsData exits; // 自動對應 JSON
}
