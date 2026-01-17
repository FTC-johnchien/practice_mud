package com.example.htmlmud.domain.model.json;

import java.util.HashMap;
import java.util.Map;

// 這個物件會被序列化存入 players.state_json 和 mobs.state_json
public class LivingState {
  public int level = 1;
  public int hp;
  public int maxHp;
  public int mp;
  public int maxMp;

  // 基礎屬性
  public Map<String, Integer> attributes = new HashMap<>(); // STR, DEX, INT

  // 戰鬥狀態 (不存入 DB，或是標記 @JsonIgnore)
  public transient boolean isDead = false;

  // 建構子與輔助方法...
  public boolean isDead() {
    return hp <= 0;
  }
}
