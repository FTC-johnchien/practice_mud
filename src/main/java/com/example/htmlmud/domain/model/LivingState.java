package com.example.htmlmud.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

// 這個物件會被序列化存入 players.state_json 和 mobs.state_json
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivingState {
  public String gender;

  public int level = 1;
  public int hp;
  public int maxHp;
  public int mp;
  public int maxMp;

  // 基礎屬性
  public Map<String, Integer> attributes = new HashMap<>(); // STR, DEX, INT

  // 戰鬥狀態 (不存入 DB，或是標記 @JsonIgnore)
  @JsonIgnore
  public transient boolean isDead = false;

  // 建構子與輔助方法...
  @JsonIgnore
  public boolean isDead() {
    return hp <= 0;
  }

  public LivingState deepCopy() {
    LivingState copy = new LivingState();
    copy.level = this.level;
    copy.hp = this.hp;
    copy.maxHp = this.maxHp;
    copy.mp = this.mp;
    copy.maxMp = this.maxMp;
    // Map 也要複製
    if (this.attributes != null) {
      copy.attributes = new HashMap<>(this.attributes);
    }
    return copy;
  }
}
