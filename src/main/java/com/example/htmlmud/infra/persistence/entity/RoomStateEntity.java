package com.example.htmlmud.infra.persistence.entity;

import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.htmlmud.domain.model.entity.GameItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "rooms_state")
@IdClass(RoomStateId.class)
@Data
public class RoomStateEntity {

  @Id
  @Column(name = "room_id", nullable = false)
  private String roomId;

  @Id
  @Column(name = "zone_id", nullable = false)
  private String zoneId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dropped_items_json")
  private List<GameItem> droppedItems;

}
