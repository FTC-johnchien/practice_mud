package com.example.htmlmud.infra.persistence.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.htmlmud.domain.model.json.LivingState;
import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class PlayerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true)
  public String username;

  public String passwordHash;
  public String nickname;
  public Long currentRoomId;

  // 關鍵：自動將 Java 物件序列化為 MySQL JSON
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "state_json")
  public LivingState state;

  // FastUtil 對 Entity 層幫助不大，因為 Hibernate 映射需要標準 Map
  // 但在 Actor 內部我們會轉換
}
