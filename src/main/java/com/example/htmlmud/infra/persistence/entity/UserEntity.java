package com.example.htmlmud.infra.persistence.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

  private String id;
  private String username;
  private String passwordHash;
  private LocalDateTime createdAt;
  private LocalDateTime lastLoginAt;
}
