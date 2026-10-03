package com.example.htmlmud.domain.dungeon.battle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import lombok.extern.slf4j.Slf4j;

/**
 * 戰鬥結果單向套用器 (Battle Outcome Applier)
 * 負責在 Actor 執行緒中安全地將不可變 BattleOutcome 單向結算至 Player 實體，
 * 同時透過 Idempotency Key 防禦重放攻擊與重複發獎漏洞。
 */
@Slf4j
@Service
public class BattleOutcomeApplier {

  private static final int MAX_IDEMPOTENCY_CACHE = 2000;

  // LRU 緩存以保存最近套用的 idempotencyKey
  private final Set<String> processedKeys = Collections.newSetFromMap(
      new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
          return size() > MAX_IDEMPOTENCY_CACHE;
        }
      }
  );

  /**
   * 套用戰鬥結果至 Player
   * @return true 若套用成功；false 若為重複套用 (idempotency key 已存在) 或參數為空
   */
  public synchronized boolean apply(Player player, BattleOutcome outcome) {
    if (player == null || outcome == null) {
      log.warn("Cannot apply null outcome or to null player");
      return false;
    }

    String key = outcome.idempotencyKey();
    if (key != null && !processedKeys.add(key)) {
      log.warn("Battle outcome with idempotencyKey [{}] has already been processed for player [{}]. Skipping.",
          key, player.getName());
      return false;
    }

    LivingStats stats = player.getStats();
    if (stats != null) {
      // 1. 同步等級與經驗值
      if (outcome.leveledUp()) {
        stats.setLevel(outcome.newLevel());
      }
      if (outcome.expGained() > 0) {
        stats.setExp((int) Math.min(Integer.MAX_VALUE, stats.getExp() + outcome.expGained()));
      }

      // 2. 套用屬性與自由分配點數
      if (outcome.freeStatPointsGained() > 0) {
        stats.setFreeStatPoints(stats.getFreeStatPoints() + outcome.freeStatPointsGained());
      }
      if (outcome.statIncreases() != null && !outcome.statIncreases().isEmpty()) {
        outcome.statIncreases().forEach((stat, delta) -> {
          if (delta != null && delta > 0) {
            applyStatDelta(stats, stat, delta);
          }
        });
      }

      // 3. 同步生命與真元（確保在合理邊界內）
      if (outcome.finalHp() >= 0) {
        stats.setHp(Math.min(stats.getMaxHp(), Math.max(0, outcome.finalHp())));
      }
      if (outcome.finalMp() >= 0) {
        stats.setMp(Math.min(stats.getMaxMp(), Math.max(0, outcome.finalMp())));
      }
    }

    // 4. 廣播公告
    if (outcome.announcements() != null && !outcome.announcements().isEmpty()) {
      for (String msg : outcome.announcements()) {
        player.reply(msg);
      }
    }

    log.debug("Applied battle outcome [{}] to Player [{}] (victory={}, expGained={}, leveledUp={})",
        key, player.getName(), outcome.victory(), outcome.expGained(), outcome.leveledUp());
    return true;
  }

  public synchronized boolean isProcessed(String idempotencyKey) {
    return idempotencyKey != null && processedKeys.contains(idempotencyKey);
  }

  public synchronized void clearHistory() {
    processedKeys.clear();
  }

  private void applyStatDelta(LivingStats stats, String statName, int delta) {
    switch (statName.toUpperCase()) {
      case "STR" -> stats.setStr(stats.getStr() + delta);
      case "CON" -> stats.setCon(stats.getCon() + delta);
      case "DEX" -> stats.setDex(stats.getDex() + delta);
      case "INT", "INTELLIGENCE" -> stats.setIntelligence(stats.getIntelligence() + delta);
      case "WIS" -> stats.setWis(stats.getWis() + delta);
      case "HP", "MAXHP" -> stats.setMaxHp(stats.getMaxHp() + delta);
      case "MP", "MAXMP" -> stats.setMaxMp(stats.getMaxMp() + delta);
      default -> log.debug("Unknown stat increase: {}", statName);
    }
  }
}
