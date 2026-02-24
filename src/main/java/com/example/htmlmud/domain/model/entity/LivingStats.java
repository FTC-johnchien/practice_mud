package com.example.htmlmud.domain.model.entity;

import java.util.HashMap;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.Gender;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 這個物件會被序列化存入 players.stats_json 和 mobs.stats_json
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivingStats {
  public Gender gender;
  public String race = "DRAGON";

  public int level = 1;
  public int age = 12;
  public int maxAge = 100;

  public int hp = 100;
  public int maxHp = 100;
  public int mp = 50;
  public int maxMp = 50;
  public int stamina = 100;
  public int maxStamina = 100;

  // private int qi = 50; // 氣 (用於武功傷害計算)
  // private int maxQi = 50; // 最大氣值
  // private int san = 100; // 理智值
  // private int maxSan = 100; // 最大理智值


  public int coin = 0; // 錢幣
  public int exp = 0; // 經驗值
  public int combatExp = 0; // 戰鬥經驗值
  public int potential; // 潛力 (影響 學習特定高級武功的限制)


  // 基礎屬性
  public int str = 5; // 力量 strength (影響 物理傷害、角色負重上限)
  public int intelligence = 5; // 智力 intelligence (影響 魔法傷害、法力上限MP、學習技能的速度)
  public int dex = 5; // 靈巧 dexterity (影響 物理威力 命中率Hit Rate、盜賊技能成功率、遠程武器瞄準（弓箭）、暗器投擲、雙手武器的協調性)
  public int con = 5; // 體質 constitution (影響 生命值HP上限、防禦力、體力恢復速度)

  // private int wis = 5; // 智慧 wisdom (影響 魔法威力)
  // private int agi = 5; // 敏捷 agility (影響 移動速度、躲避率Evasion 閃避攻擊、戰鬥中的出手順序（主動性）。)
  // private int cha; // 魅力 charisma
  // private int luk; // 福緣 luck
  // private int karma; // 因果 karma
  // private int spi; // 靈性 spirit
  // private int cou; // 膽識 courage
  // private int conce; // 定力 concentration
  // private int app; // 容貌 appearance

  // private int reputation; // 名聲 (影響 與 NPC 的互動)



  // === 裝備欄位 ===
  public Map<EquipmentSlot, GameItem> equipment = new HashMap<>();
  // 已學到的技能
  public Map<String, SkillEntry> learnedSkills = new HashMap<>();
  // 已裝備的技能
  public Map<SkillCategory, String> enabledSkills = new HashMap<>();


  // 數值意義：0.0 = 無抗性, 0.5 = 減傷 50%, -0.5 = 增傷 50% (弱點), 1.0 = 免疫。
  public transient Map<DamageType, Double> resistances = new HashMap<>();



  // 動態戰鬥資源 (不需要存檔，戰鬥結束清除)
  // Key: "charge", "combo_points", "rage"
  private transient Map<String, Integer> combatResources = new HashMap<>();



  public LivingStats deepCopy() {
    LivingStats copy = new LivingStats();
    copy.gender = this.gender;
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

    copy.coin = this.coin;
    copy.exp = this.exp;
    copy.combatExp = this.combatExp;
    copy.potential = this.potential;



    copy.str = this.str;
    copy.intelligence = this.intelligence;
    copy.dex = this.dex;
    copy.con = this.con;

    return copy;
  }

  /**
   * 取得指定類型的抗性 (包含父類別抗性的加總)
   */
  public double getResistance(DamageType type) {
    double res = resistances.getOrDefault(type, 0.0);
    if (type.getParent() != null) {
      res += getResistance(type.getParent());
    }
    return res;
  }

  // --- 用於 ResourceType.CHARGE 的方法 ---
  public int getCombatResource(String key) {
    return combatResources.getOrDefault(key, 0);
  }

  public void modifyCombatResource(String key, int delta) {
    combatResources.merge(key, delta, Integer::sum);
    // 如果減到 0 或以下，移除該 key 以節省空間 (可選)
    if (combatResources.get(key) <= 0) {
      combatResources.remove(key);
    }
  }

}
