package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;
import com.example.htmlmud.domain.service.XpProgressionService;
import lombok.extern.slf4j.Slf4j;

/**
 * MUD 角色修為狀態與道基自由潛能點分配指令 (Stat / Score Command)
 * 支援透過文字選單查看六維道行，並進行自由潛能分配，雙向同步至小隊與前端 DRPG 介面。
 */
@Slf4j
@Component
@CommandAlias({"score", "status", "狀態", "加點", "屬性"})
public class StatCommand implements PlayerCommand {

  private final XpProgressionService xpProgressionService;
  private final PartyService partyService;
  private final CharacterSyncService characterSyncService;
  private final GameStateBroadcastService broadcastService;

  @org.springframework.beans.factory.annotation.Autowired
  public StatCommand(XpProgressionService xpProgressionService,
      @org.springframework.context.annotation.Lazy PartyService partyService,
      @org.springframework.context.annotation.Lazy CharacterSyncService characterSyncService,
      @org.springframework.context.annotation.Lazy GameStateBroadcastService broadcastService) {
    this.xpProgressionService = xpProgressionService;
    this.partyService = partyService;
    this.characterSyncService = characterSyncService;
    this.broadcastService = broadcastService;
  }

  public StatCommand(XpProgressionService xpProgressionService,
      PartyService partyService,
      CharacterSyncService characterSyncService) {
    this(xpProgressionService, partyService, characterSyncService, null);
  }

  public StatCommand() {
    this(new XpProgressionService(), null, null, null);
  }

  @Override
  public String getKey() {
    return "stat";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    if (self == null || self.getStats() == null) {
      return;
    }

    String input = args != null ? args.trim() : "";
    if (input.isEmpty() || input.equalsIgnoreCase("list") || input.equalsIgnoreCase("view") || input.equalsIgnoreCase("show")) {
      self.reply(formatPlayerStats(self));
      return;
    }

    String[] parts = input.split("\\s+");
    String first = parts[0].toLowerCase();

    String statName;
    int amount = 1;

    if (first.equals("add") || first.equals("allocate") || first.equals("加點")) {
      if (parts.length < 2) {
        self.reply("用法: stat add <str|con|dex|int|wis> [點數]\n例如: stat add con 2 或 stat add str 1");
        return;
      }
      statName = parts[1];
      if (parts.length >= 3) {
        try {
          amount = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
          self.reply("請輸入正確的點數，例如: stat add str 2");
          return;
        }
      }
    } else if (isStatKey(first)) {
      statName = first;
      if (parts.length >= 2) {
        try {
          amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          self.reply("請輸入正確的點數，例如: stat str 2");
          return;
        }
      }
    } else {
      self.reply("未知的屬性或子指令。可用指令：\n  stat (檢視修為狀態)\n  stat add <str|con|dex|int|wis> [點數] (自由加點)");
      return;
    }

    if (amount <= 0) {
      self.reply("加點數值必須大於 0！");
      return;
    }

    LivingStats stats = self.getStats();
    if (stats.getFreeStatPoints() < amount) {
      self.reply("【自由點數不足】當前僅有 " + stats.getFreeStatPoints() + " 點可用！");
      return;
    }

    boolean success = xpProgressionService.allocateStatPoint(self, statName, amount);
    if (!success) {
      self.reply("無效的屬性名稱，可選屬性：str (力量), con (根骨), dex (身法), int (悟性), wis (定力)");
      return;
    }

    // 雙向同步至 Party 隊長 (若隊伍存在)
    syncToPartyLeader(self);

    self.reply("【道胎拓寬】成功將 " + amount + " 點自由點數注入【" + statName.toUpperCase() + "】！剩餘點數：" + stats.getFreeStatPoints() + " 點。");

    if (broadcastService != null) {
      broadcastService.broadcastState(self);
    }
  }

  private boolean isStatKey(String key) {
    if (key == null) return false;
    String k = key.toLowerCase();
    return switch (k) {
      case "str", "力量", "臂力",
           "con", "根骨", "體質",
           "dex", "靈巧", "身法",
           "int", "intelligence", "悟性", "智力",
           "wis", "定力", "精神" -> true;
      default -> false;
    };
  }

  private void syncToPartyLeader(Player self) {
    if (partyService == null) return;
    try {
      Party party = partyService.getOrCreateParty(self);
      if (party != null && party.getLeader() != null) {
        PartyMember leader = party.getLeader();
        if (characterSyncService != null) {
          characterSyncService.syncFromPlayerToParty(self, leader);
        } else {
          LivingStats pStats = self.getStats();
          LivingStats lStats = leader.getStats();
          if (pStats != null && lStats != null) {
            lStats.setStr(pStats.getStr());
            lStats.setCon(pStats.getCon());
            lStats.setDex(pStats.getDex());
            lStats.setIntelligence(pStats.getIntelligence());
            lStats.setWis(pStats.getWis());
            lStats.setMaxHp(pStats.getMaxHp());
            lStats.setHp(pStats.getHp());
            lStats.setMaxMp(pStats.getMaxMp());
            lStats.setMp(pStats.getMp());
            lStats.setFreeStatPoints(pStats.getFreeStatPoints());
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to sync player stats to party leader: {}", e.getMessage());
    }
  }

  public String formatPlayerStats(Player player) {
    LivingStats stats = player.getStats();
    String className = player.getClassTemplate().map(ClassTemplate::name).orElse(player.getClassId());
    String nickname = player.getNickname() != null && !player.getNickname().isBlank()
        ? "【" + player.getNickname() + "】"
        : "";

    StringBuilder sb = new StringBuilder();
    sb.append("═════════════【道途修為・角色狀態】═════════════\n");
    sb.append("  名號：").append(player.getName()).append(nickname)
      .append("  門派職業：").append(className).append(" (Lv.").append(stats.getLevel()).append(")\n");
    sb.append("  道基修為：[").append(stats.getExp()).append(" / ").append(stats.getNextLevelExp()).append(" EXP]\n");
    sb.append("  氣血真元：HP [").append(stats.getHp()).append("/").append(stats.getMaxHp()).append("]  ")
      .append("MP [").append(stats.getMp()).append("/").append(stats.getMaxMp()).append("]\n");
    sb.append("  精力理智：SP [").append(stats.getStamina()).append("/").append(stats.getMaxStamina()).append("]  ")
      .append("SAN [").append(stats.getSan()).append("/").append(stats.getMaxSan()).append("]\n");
    sb.append("  未分配自由潛能點：【").append(stats.getFreeStatPoints()).append("】點\n");
    sb.append("──────────────────────────────────────────────\n");
    sb.append("  [STR] 力量/臂力：").append(stats.getStr()).append(" 點 (影響物理傷害、穿透與負重)\n");
    sb.append("  [CON] 根骨/體質：").append(stats.getCon()).append(" 點 (影響氣血上限 HP: ").append(stats.getMaxHp()).append("、減傷)\n");
    sb.append("  [DEX] 靈巧/身法：").append(stats.getDex()).append(" 點 (影響暴擊、命中、閃避迴避)\n");
    sb.append("  [INT] 悟性/智力：").append(stats.getIntelligence()).append(" 點 (影響真元上限 MP: ").append(stats.getMaxMp()).append("、法術傷害)\n");
    sb.append("  [WIS] 定力/精神：").append(stats.getWis()).append(" 點 (影響治療效果、法力恢復、道心抗性)\n");
    sb.append("──────────────────────────────────────────────\n");
    sb.append("  加點指令：stat add <str|con|dex|int|wis> [點數] (或 score add <屬性> [點數])\n");
    sb.append("══════════════════════════════════════════════");
    return sb.toString();
  }

  @Override
  public String getDescription() {
    return "檢視道途修為、六維屬性與自由潛能加點 (score / stat)";
  }
}
