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

  // === 新增戰鬥屬性 ===
  // 基礎屬性
  public int str; // 力量 (影響物理攻擊)
  public int dex; // 敏捷 (影響命中/閃避)
  public int con; // 體質 (影響 HP)

  // 基礎屬性
  // public Map<String, Integer> attributes = new HashMap<>(); // STR, DEX, INT

  // 衍生屬性 (通常不存 DB，由基礎屬性計算，但為了簡單先存這裡)
  public int attackPower; // 攻擊力
  public int defense; // 防禦力
  public int attackSpeed; // 攻擊速度 (毫秒，例如 2000 代表 2秒打一次)

  // === 戰鬥狀態 ===
  @JsonIgnore
  public transient boolean isInCombat = false;

  @JsonIgnore
  public transient long nextAttackTime = 0; // 下一次可以攻擊的時間點 (System.currentTimeMillis)

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
    // if (this.attributes != null) {
    // copy.attributes = new HashMap<>(this.attributes);
    // }
    copy.str = this.str;
    copy.dex = this.dex;
    copy.con = this.con;
    copy.attackPower = this.attackPower;
    copy.defense = this.defense;
    copy.attackSpeed = this.attackSpeed;
    // copy.isDead = this.isDead;
    // copy.isInCombat = this.isInCombat;
    // copy.nextAttackTime = this.nextAttackTime;
    return copy;
  }
}
