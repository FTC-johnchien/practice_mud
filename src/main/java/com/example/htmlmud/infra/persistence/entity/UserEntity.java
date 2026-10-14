package com.example.htmlmud.infra.persistence.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

  public UserEntity() {}

  @Builder
  public UserEntity(String id, String username, String passwordHash, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
    this.lastLoginAt = lastLoginAt;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  private LocalDateTime createdAt;

  private LocalDateTime lastLoginAt;

  // 這裡甚至不需要關聯 Character 列表，除非後台管理需要
}
