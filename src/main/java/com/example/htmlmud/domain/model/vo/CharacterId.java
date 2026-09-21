package com.example.htmlmud.domain.model.vo;

import java.util.Objects;
import java.util.UUID;
import com.example.htmlmud.domain.actor.impl.Player;

/**
 * 強型別角色與會話識別物件 (Character Identity Value Object)
 * 用於統一 MUD 與 DRPG 中的玩家狀態索引，消除名稱與 ID 分裂風險。
 */
public record CharacterId(String value) {

  public CharacterId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("CharacterId cannot be null or blank");
    }
  }

  public static CharacterId of(String value) {
    if (value == null || value.isBlank()) {
      return of("unknown");
    }
    return new CharacterId(value.trim());
  }

  public static CharacterId fromPlayer(Player player) {
    if (player == null) {
      return of("unknown");
    }
    String id = player.getId();
    if (id != null && !id.isBlank() && !id.equals("p-single")) {
      return of(id);
    }
    String name = player.getName();
    if (name != null && !name.isBlank()) {
      return of(name);
    }
    return of(id != null && !id.isBlank() ? id : "unknown");
  }

  public static CharacterId generate(String prefix) {
    String p = (prefix != null && !prefix.isBlank()) ? prefix : "char";
    return of(p + "-" + UUID.randomUUID().toString().substring(0, 8));
  }

  @Override
  public String toString() {
    return value;
  }
}
