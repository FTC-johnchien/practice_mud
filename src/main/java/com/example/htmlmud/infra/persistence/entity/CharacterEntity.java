package com.example.htmlmud.infra.persistence.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
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
@Table(name = "characters",
    indexes = {@Index(name = "idx_uid_name", columnList = "uid,name", unique = true)})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  // 邏輯關聯 (這裡存 ID 比較簡單，避免 N+1 或 Lazy Loading 問題)
  @Column(nullable = false)
  private String uid;

  private String name;

  private String nickname;

  @Column(name = "look_description")
  private String lookDescription;

  @Column(name = "current_room_id")
  private String currentRoomId;

  // 關鍵：自動將 Java 物件序列化為 MySQL JSON
  // 使用 MySQL 8.4 JSON 類型儲存擴充資料 (例如: HP, Mana, EXP, 背包)
  // 這樣未來新增屬性不用一直改 Table Schema
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "state_json")
  private LivingStats state;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "inventory_json")
  private List<GameItem> inventory; // 背包系統

  // @JdbcTypeCode(SqlTypes.JSON)
  // @Column(name = "skills_json")
  // public PlayerSkills skills; // 技能系統 對應 skills_json

  // @JdbcTypeCode(SqlTypes.JSON)
  // @Column(name = "config_json")
  // public PlayerConfig config; // 設定檔 對應 config_json

  private LocalDateTime createdAt;

  private LocalDateTime modifyAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (currentRoomId == null)
      currentRoomId = "newbie_village:entrance"; // 預設新手村
  }
}
