package com.example.htmlmud.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

// 用於唯一識別遊戲中的任何物件
public record GameObjectId(Type type, long id) {

  public enum Type {
    PLAYER, MOB, NPC, ITEM
  }

  // 方便轉成字串 key (例如 "PLAYER:1001") 用於 Map 或 Log
  @Override
  @JsonValue
  public String toString() {
    return type + ":" + id;
  }

  // 輔助建構子
  public static GameObjectId player(long id) {
    return new GameObjectId(Type.PLAYER, id);
  }

  public static GameObjectId mob(long id) {
    return new GameObjectId(Type.MOB, id);
  }
}
