package com.example.htmlmud.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

// 這個物件會被序列化存入 players.state_json 和 mobs.state_json
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivingState {
  public String sex = "F"; // 性別
  public String race = "DRAGON"; // 種族 (人類、精靈等，通常會給予不同的初始屬性加成)

  public int level = 1;
  public int age = 12; // 年齡 age
  public int maxAge = 100; // 最大年齡
  public int hp = 100;
  public int maxHp = 100;
  public int mp = 50;
  public int maxMp = 50;
  public int stamina = 100; // 體力值
  public int maxStamina = 100; // 最大體力值

  private int qi = 50; // 氣 (用於武功傷害計算)
  private int maxQi = 50; // 最大氣值
  private int san = 100; // 理智值
  private int maxSan = 100; // 最大理智值


  public int exp = 0; // 經驗值
  public int combatExp = 0; // 戰鬥經驗值


  // 基礎屬性
  public int str = 5; // 力量 strength (影響 物理傷害、角色負重上限)
  public int intelligence = 5; // 智力 intelligence (影響 魔法傷害、法力上限MP、學習技能的速度)
  public int agi = 5; // 敏捷 agility (影響 攻擊速度、躲避率Evasion、命中率Hit Rate)
  public int con = 5; // 體質 constitution (影響 生命值HP上限、防禦力、體力恢復速度)

  private int wis = 5; // 智慧 wisdom (影響 魔法威力)
  private int dex = 5; // 靈巧 dexterity (影響 物理威力)
  private int cha; // 魅力 charisma
  private int luk; // 福緣 luck
  private int karma; // 因果 karma
  private int spi; // 靈性 spirit
  private int cou; // 膽識 courage
  private int conce; // 定力 concentration
  private int app; // 容貌 appearance

  private int reputation; // 名聲 (影響 與 NPC 的互動)
  private int potential; // 潛力 (影響 學習特定高級武功的限制)


  // === 戰鬥狀態 ===
  @JsonIgnore
  public transient boolean isInCombat = false;
  // 當前鎖定的攻擊目標 ID (null 代表沒在打架)
  @JsonIgnore
  public transient String combatTargetId;

  @JsonIgnore
  public transient long nextAttackTime = 0; // 下一次可以攻擊的時間點 (System.currentTimeMillis)

  // 戰鬥狀態 (不存入 DB，或是標記 @JsonIgnore)
  // @JsonIgnore
  // public transient boolean isDead = false;


  // === 裝備欄位 ===
  public Map<EquipmentSlot, GameItem> equipment = new HashMap<>();

  // 衍生屬性 (快取用，每次穿脫裝備後重新計算 通常不存 DB，由基礎屬性計算，但為了簡單先存這裡)
  public transient int minDamage = str; // 最小傷害
  public transient int maxDamage = str; // 最大傷害
  public transient int defense = 0; // 防禦力
  public transient int attackSpeed = 2000; // 攻擊速度 (毫秒，例如 2000 代表 2秒打一次)
  public transient int weightCapacity = str * 10;

  // 建構子與輔助方法...
  @JsonIgnore
  public boolean isDead() {
    return hp <= 0;
  }

  // 判斷是否在戰鬥中
  @JsonIgnore
  public boolean isInCombat() {
    return isInCombat && combatTargetId != null;
  }

  public boolean isBusy() {
    return (isInCombat) ? isInCombat : false;
  }

  public LivingState deepCopy() {
    LivingState copy = new LivingState();
    copy.sex = this.sex;
    copy.race = this.race;

    copy.level = this.level;
    copy.age = this.age;
    copy.maxAge = this.maxAge;
    copy.hp = this.hp;
    copy.maxHp = this.maxHp;
    copy.mp = this.mp;
    copy.maxMp = this.maxMp;
    copy.stamina = this.stamina;
    copy.maxStamina = this.maxStamina;

    copy.exp = this.exp;
    copy.combatExp = this.combatExp;



    // Map 也要複製
    // if (this.attributes != null) {
    // copy.attributes = new HashMap<>(this.attributes);
    // }
    copy.str = this.str;
    copy.intelligence = this.intelligence;
    copy.agi = this.agi;
    copy.con = this.con;

    // copy.damage = this.damage;
    // copy.defense = this.defense;
    // copy.attackSpeed = this.attackSpeed;
    // copy.weightCapacity = this.weightCapacity;

    return copy;
  }
}
