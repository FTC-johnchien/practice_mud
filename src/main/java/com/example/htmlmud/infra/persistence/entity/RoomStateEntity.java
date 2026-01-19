package com.example.htmlmud.infra.persistence.entity;

import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.map.RoomExit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "rooms_state")
@Data
public class RoomStateEntity {

  @Id
  @Column(nullable = false, columnDefinition = "INT(11) UNSIGNED")
  private long id;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dropped_items_json")
  private List<GameItem> droppedItems;

}
