package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.PartyMember;

@Service
public class XpProgressionService {

  public record LevelUpResult(
      String memberName,
      int oldLevel,
      int newLevel,
      boolean isLeader,
      int freeStatPointsGained,
      Map<String, Integer> statIncreases
  ) {
    public boolean isLeveledUp() {
      return newLevel > oldLevel;
    }

    public String formatAnnouncement() {
      StringBuilder sb = new StringBuilder();
      sb.append("【金光灌頂】「").append(memberName).append("」突破境界！(Lv.").append(oldLevel).append(" ➔ Lv.").append(newLevel).append(")");
      if (statIncreases != null && !statIncreases.isEmpty()) {
        sb.append(" 基礎屬性成長: ");
        List<String> diffs = new ArrayList<>();
        statIncreases.forEach((k, v) -> {
          if (v > 0) diffs.add(k.toUpperCase() + "+" + v);
        });
        sb.append(String.join(", ", diffs));
      }
      if (freeStatPointsGained > 0) {
        sb.append("，獲得自由修為點數 +").append(freeStatPointsGained).append("！");
      }
      return sb.toString();
    }
  }

  public record SkillLevelUpResult(
      String skillId,
      String skillName,
      int oldLevel,
      int newLevel,
      long currentXp,
      long requiredXp,
      boolean leveledUp
  ) {
    public String formatAnnouncement() {
      return "你的技能【" + skillName + "】突破精進至 Lv." + newLevel + "！";
    }
  }

  /**
   * 晉升下一級所需經驗值曲線：
   * Level 1 -> 180
   * NextLevelExp(L) = floor(60 * L^1.6 + 120 * L)
   */
  public long calculateNextLevelExp(int level) {
    if (level <= 1) {
      return 180L;
    }
    return Math.max(180L, (long) Math.floor(60.0 * Math.pow(level, 1.6) + 120.0 * level));
  }

  /**
   * 計算技能晉級所需熟練度 (統一公式)
   * 基礎公式：50 * level^2 * learningDifficulty
   */
  public long calculateSkillRequiredExp(int skillLevel, double difficulty) {
    int lv = Math.max(1, skillLevel);
    double diff = difficulty > 0 ? difficulty : 1.0;
    long baseReq = 50L * (long) Math.pow(lv, 2);
    return Math.max(50L, (long) (baseReq * diff));
  }

  /**
   * 計算技能晉級所需熟練度 (以 SkillEntry 與 SkillTemplate 為準)
   */
  public long calculateSkillRequiredExp(SkillEntry userSkill, SkillTemplate template) {
    if (userSkill == null) return 50L;
    int lv = userSkill.getLevel();
    double difficulty = (template != null && template.getMechanics() != null)
        ? template.getMechanics().learningDifficulty()
        : 1.0;
    return calculateSkillRequiredExp(lv, difficulty);
  }

  /**
   * 為技能增加熟練度並判定升級
   */
  public SkillLevelUpResult awardSkillExp(SkillEntry entry, SkillTemplate template, long expGained) {
    if (entry == null || expGained <= 0) {
      return new SkillLevelUpResult("", "", 0, 0, 0, 0, false);
    }
    String skillId = template != null ? template.getId() : "";
    String skillName = (template != null && template.getName() != null) ? template.getName() : skillId;
    int maxLevel = (template != null && template.getMechanics() != null)
        ? template.getMechanics().maxLevel()
        : 100;

    int oldLevel = entry.getLevel();
    if (oldLevel >= maxLevel) {
      return new SkillLevelUpResult(skillId, skillName, oldLevel, oldLevel, entry.getXp(), 0, false);
    }

    entry.addXp(expGained);
    int currentLevel = oldLevel;

    while (currentLevel < maxLevel) {
      double diff = (template != null && template.getMechanics() != null) ? template.getMechanics().learningDifficulty() : 1.0;
      long needed = calculateSkillRequiredExp(currentLevel, diff);
      if (entry.getXp() >= needed) {
        long leftover = entry.getXp() - needed;
        entry.levelUp();
        entry.setXp(leftover);
        currentLevel = entry.getLevel();
      } else {
        break;
      }
    }

    double diff = (template != null && template.getMechanics() != null) ? template.getMechanics().learningDifficulty() : 1.0;
    long nextNeeded = (currentLevel < maxLevel) ? calculateSkillRequiredExp(currentLevel, diff) : 0L;

    return new SkillLevelUpResult(skillId, skillName, oldLevel, currentLevel, entry.getXp(), nextNeeded, currentLevel > oldLevel);
  }

  /**
   * 計算怪物擊殺獎勵經驗值 (統一 MUD / DRPG 戰鬥經驗掉落標準)
   */
  public int calculateMobExpReward(int mobLevel, int playerLevel) {
    int mLv = Math.max(1, mobLevel);
    int pLv = Math.max(1, playerLevel);
    int baseExp = mLv * 15;
    int levelDiff = mLv - pLv;
    if (levelDiff > 0) {
      baseExp += Math.min(baseExp, levelDiff * 5);
    } else if (levelDiff < -5) {
      baseExp = Math.max(5, baseExp + levelDiff * 2);
    }
    return Math.max(5, baseExp);
  }

  /**
   * 為 MUD 玩家主角發放經驗值並判定升級
   */
  public LevelUpResult awardExp(Player player, long expGained) {
    if (player == null || player.getStats() == null || expGained <= 0) {
      return new LevelUpResult(player != null ? player.getName() : "", 0, 0, true, 0, Map.of());
    }

    LivingStats stats = player.getStats();
    int oldLevel = stats.getLevel();
    if (oldLevel <= 0) {
      oldLevel = 1;
      stats.setLevel(1);
    }
    if (stats.getNextLevelExp() <= 0) {
      stats.setNextLevelExp(calculateNextLevelExp(oldLevel));
    }

    stats.setExp((int) Math.min(Integer.MAX_VALUE, stats.getExp() + expGained));

    int currentLevel = oldLevel;
    int freePointsEarned = 0;
    Map<String, Integer> totalStatDeltas = new LinkedHashMap<>();

    while (stats.getExp() >= stats.getNextLevelExp() && currentLevel < 1000) {
      stats.setExp((int) (stats.getExp() - stats.getNextLevelExp()));
      currentLevel++;
      stats.setLevel(currentLevel);
      stats.setNextLevelExp(calculateNextLevelExp(currentLevel));

      // 玩家基礎屬性成長 (HP +25, MP +5, 體質+1, 力量+1)
      stats.setMaxHp(stats.getMaxHp() + 25);
      stats.setMaxMp(stats.getMaxMp() + 5);
      stats.setCon(stats.getCon() + 1);
      stats.setStr(stats.getStr() + 1);
      totalStatDeltas.merge("CON", 1, Integer::sum);
      totalStatDeltas.merge("STR", 1, Integer::sum);

      // 主角專屬 +2 自由分配點數
      stats.setFreeStatPoints(stats.getFreeStatPoints() + 2);
      freePointsEarned += 2;

      // 升級狀態回滿
      stats.setHp(stats.getMaxHp());
      stats.setMp(stats.getMaxMp());
    }

    LevelUpResult result = new LevelUpResult(player.getName(), oldLevel, currentLevel, true, freePointsEarned, totalStatDeltas);
    if (result.isLeveledUp() && player.isValid()) {
      player.reply("\u001B[1;33m" + result.formatAnnouncement() + "\u001B[0m");
    }
    return result;
  }

  /**
   * 為隊員發放經驗值並判定升級
   */
  public LevelUpResult awardExp(PartyMember member, long expGained) {
    if (member == null || member.getStats() == null || expGained <= 0) {
      return new LevelUpResult(member != null ? member.getName() : "", 0, 0, false, 0, Map.of());
    }

    LivingStats stats = member.getStats();
    int oldLevel = stats.getLevel();
    if (oldLevel <= 0) {
      oldLevel = 1;
      stats.setLevel(1);
    }
    if (stats.getNextLevelExp() <= 0) {
      stats.setNextLevelExp(calculateNextLevelExp(oldLevel));
    }

    stats.setExp((int) Math.min(Integer.MAX_VALUE, stats.getExp() + expGained));

    int currentLevel = oldLevel;
    int freePointsEarned = 0;
    Map<String, Integer> totalStatDeltas = new LinkedHashMap<>();
    boolean isLeader = member.isLeader();

    while (stats.getExp() >= stats.getNextLevelExp() && currentLevel < 1000) {
      stats.setExp((int) (stats.getExp() - stats.getNextLevelExp()));
      int prevL = currentLevel;
      currentLevel++;
      stats.setLevel(currentLevel);
      stats.setNextLevelExp(calculateNextLevelExp(currentLevel));

      // 應用職業基礎成長
      applyLevelGrowth(member, prevL, currentLevel, totalStatDeltas);

      // 主角額外獲得 +2 自由分配點數
      if (isLeader) {
        stats.setFreeStatPoints(stats.getFreeStatPoints() + 2);
        freePointsEarned += 2;
      }

      // 升級狀態回滿
      stats.setHp(stats.getMaxHp());
      stats.setMp(stats.getMaxMp());
    }

    return new LevelUpResult(member.getName(), oldLevel, currentLevel, isLeader, freePointsEarned, totalStatDeltas);
  }

  /**
   * 應用職業自適應成長數值
   */
  private void applyLevelGrowth(PartyMember member, int fromLevel, int toLevel, Map<String, Integer> totalDeltas) {
    LivingStats stats = member.getStats();
    int hpGain = 25;
    int mpGain = 5;
    Map<String, Double> weights = Map.of(
        "CON", 2.0,
        "STR", 1.5,
        "DEX", 1.0,
        "INT", 0.5,
        "WIS", 0.0
    );

    var classOpt = member.getClassTemplate();
    if (classOpt.isPresent() && classOpt.get().growth() != null) {
      ClassTemplate.ClassGrowth growth = classOpt.get().growth();
      hpGain = growth.hpPerLevel();
      mpGain = growth.mpPerLevel();
      if (growth.statWeights() != null) {
        weights = growth.statWeights();
      }
    }

    stats.setMaxHp(stats.getMaxHp() + hpGain);
    stats.setMaxMp(stats.getMaxMp() + mpGain);

    for (Map.Entry<String, Double> entry : weights.entrySet()) {
      String key = entry.getKey().toUpperCase();
      double w = entry.getValue();
      int delta = (int) Math.floor(toLevel * w) - (int) Math.floor(fromLevel * w);
      if (delta > 0) {
        applyStatDelta(stats, key, delta);
        totalDeltas.merge(key, delta, Integer::sum);
      }
    }
  }

  private void applyStatDelta(LivingStats stats, String statKey, int delta) {
    switch (statKey) {
      case "CON" -> stats.setCon(stats.getCon() + delta);
      case "STR" -> stats.setStr(stats.getStr() + delta);
      case "DEX" -> stats.setDex(stats.getDex() + delta);
      case "INT" -> stats.setIntelligence(stats.getIntelligence() + delta);
      case "WIS" -> stats.setWis(stats.getWis() + delta);
    }
  }

  /**
   * 主角自由分配屬性點數
   */
  public boolean allocateStatPoint(PartyMember leader, String statName, int amount) {
    if (leader == null || leader.getStats() == null || amount <= 0) {
      return false;
    }
    LivingStats stats = leader.getStats();
    if (stats.getFreeStatPoints() < amount) {
      return false;
    }

    String lower = statName.trim().toLowerCase();
    switch (lower) {
      case "str", "力量", "臂力" -> stats.setStr(stats.getStr() + amount);
      case "con", "根骨", "體質" -> {
        stats.setCon(stats.getCon() + amount);
        stats.setMaxHp(stats.getMaxHp() + amount * 10);
        stats.setHp(stats.getHp() + amount * 10);
      }
      case "dex", "靈巧", "身法" -> stats.setDex(stats.getDex() + amount);
      case "int", "intelligence", "悟性", "智力" -> {
        stats.setIntelligence(stats.getIntelligence() + amount);
        stats.setMaxMp(stats.getMaxMp() + amount * 8);
        stats.setMp(stats.getMp() + amount * 8);
      }
      case "wis", "定力", "精神" -> stats.setWis(stats.getWis() + amount);
      default -> {
        return false;
      }
    }

    stats.setFreeStatPoints(stats.getFreeStatPoints() - amount);
    return true;
  }
}
