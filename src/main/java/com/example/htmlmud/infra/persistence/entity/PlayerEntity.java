package com.example.htmlmud.infra.persistence.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.htmlmud.domain.model.json.LivingState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "players",
    indexes = {@Index(name = "idx_username", columnList = "username", unique = true)})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public String id;

  @Column(nullable = false, length = 50)
  public String username;

  @Column(nullable = false)
  public String passwordHash;

  public String nickname;

  @Column(name = "current_room_id")
  public Integer currentRoomId;

  // 關鍵：自動將 Java 物件序列化為 MySQL JSON
  // 使用 MySQL 8.4 JSON 類型儲存擴充資料 (例如: HP, Mana, EXP, 背包)
  // 這樣未來新增屬性不用一直改 Table Schema
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "state_json")
  public LivingState state;

  private LocalDateTime createdAt;

  private LocalDateTime lastLoginAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (currentRoomId == 0)
      currentRoomId = 1001; // 預設新手村
  }
}
