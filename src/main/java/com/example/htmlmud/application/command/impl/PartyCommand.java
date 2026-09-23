package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.model.TacticsCondition;
import com.example.htmlmud.domain.party.model.TacticsRule;
import com.example.htmlmud.domain.party.model.TacticsTarget;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias({"team", "coven"})
public class PartyCommand implements PlayerCommand {

  private final PartyService partyService;
  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;
  private final TemplateReader templateReader;

  @org.springframework.beans.factory.annotation.Autowired
  public PartyCommand(PartyService partyService, DungeonManager dungeonManager,
      DungeonNavigator dungeonNavigator,
      com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService,
      com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService,
      TemplateReader templateReader) {
    this.partyService = partyService;
    this.dungeonManager = dungeonManager;
    this.dungeonNavigator = dungeonNavigator;
    this.battleService = battleService;
    this.broadcastService = broadcastService;
    this.templateReader = templateReader;
  }

  public PartyCommand(PartyService partyService, DungeonManager dungeonManager,
      DungeonNavigator dungeonNavigator,
      com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService,
      com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService) {
    this(partyService, dungeonManager, dungeonNavigator, battleService, broadcastService,
        new TemplateCatalog());
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.domain.service.XpProgressionService xpProgressionService;

  public com.example.htmlmud.domain.service.XpProgressionService getXpProgressionService() {
    if (xpProgressionService == null) {
      xpProgressionService = new com.example.htmlmud.domain.service.XpProgressionService();
    }
    return xpProgressionService;
  }

  public void setXpProgressionService(com.example.htmlmud.domain.service.XpProgressionService xpProgressionService) {
    this.xpProgressionService = xpProgressionService;
  }

  public Party getOrCreateParty(String playerName) {
    return partyService.getOrCreateParty(playerName);
  }

  @Override
  public String getKey() {
    return "party";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    Party party = getOrCreateParty(self.getName());
    String input = args != null ? args.trim() : "";

    if (input.isEmpty() || input.equals("status") || input.equals("list") || input.equals("detail")) {
      self.reply(partyService.formatPartyStatus(party));
      broadcastDrpgState(self);
      return;
    }

    String[] parts = input.split("\\s+");
    String subCmd = parts[0].toLowerCase();

    switch (subCmd) {
      case "energy" -> {
        if (parts.length > 1) {
          try {
            int amount = Integer.parseInt(parts[1]);
            party.addFormationEnergy(amount);
            self.reply("【小隊靈威】小隊陣法靈威值已調整，當前為: [" + party.getFormationEnergy() + "/100]");
            broadcastDrpgState(self);
          } catch (NumberFormatException e) {
            self.reply("請輸入正確的數值，例如: party energy 50");
          }
        }
      }
      case "add" -> {
        if (parts.length < 2) {
          self.reply("用法: party add <夥伴姓名> [稱號定位] [front/back]");
          return;
        }
        if (party.size() >= Party.MAX_PARTY_SIZE) {
          self.reply("【小隊已滿】目前編制已達上限 " + Party.MAX_PARTY_SIZE + " 人！");
          return;
        }
        String name = parts[1];
        String role = parts.length > 2 ? parts[2] : "浪跡散修";
        RowPosition row = (parts.length > 3 && parts[3].equalsIgnoreCase("front")) ? RowPosition.FRONT : RowPosition.BACK;

        LivingStats s = new LivingStats();
        s.setHp(100);
        s.setMaxHp(100);
        s.setMp(50);
        s.setMaxMp(50);

        party.addMember(PartyMember.builder()
            .id("m-" + System.currentTimeMillis())
            .name(name)
            .roleTitle(role)
            .row(row)
            .stats(s)
            .baseMinDamage(10)
            .baseMaxDamage(20)
            .baseDefense(5)
            .currentSan(100)
            .maxSan(100)
            .build());
        self.reply("【招募成功】道友「" + name + "」(" + role + ") 正式加入小隊！\n" + partyService.formatPartyStatus(party));
        broadcastDrpgState(self);
      }
      case "recruit", "hire", "join" -> {
        if (parts.length < 2) {
          self.reply("用法: party recruit <夥伴ID或姓名>");
          return;
        }
        String target = parts[1];
        handleRecruit(self, party, target);
      }
      case "dismiss", "fire", "remove" -> {
        if (parts.length < 2) {
          self.reply("用法: party dismiss <夥伴ID或姓名>");
          return;
        }
        String target = parts[1];
        handleDismiss(self, party, target);
      }
      case "enable" -> {
        if (parts.length < 3) {
          self.reply("用法: party enable <隊員編號: 0~5> <技能ID>");
          return;
        }
        try {
          int mIdx = Integer.parseInt(parts[1]);
          if (mIdx < 0 || mIdx >= party.getMembers().size()) {
            self.reply("隊員編號超出範圍 (0 ~ " + (party.getMembers().size() - 1) + ")！");
            return;
          }
          String skillId = parts[2].trim();
          PartyMember m = party.getMembers().get(mIdx);
          var skillOpt = templateReader.findSkill(skillId);
          if (skillOpt.isEmpty()) {
            self.reply("找不到指定的武學或道術: " + skillId);
            return;
          }
          var skill = skillOpt.get();
          var cat = PartyMember.resolveSkillCategory(skill);
          m.enableSkill(cat, skillId);
          self.reply("【武學掛載】已將成員「" + m.getName() + "」的 [" + cat + "] 分類主修武學設定為【" + skill.getName() + "】！");
          broadcastDrpgState(self);
        } catch (NumberFormatException e) {
          self.reply("隊員編號格式錯誤，請輸入數字 (0~5)。");
        }
      }
      case "switch", "row", "pos", "stand", "站位" -> {
        if (parts.length < 2) {
          self.reply("用法: party switch <隊員編號: 0~4> [front|back]");
          return;
        }
        try {
          int mIdx = Integer.parseInt(parts[1]);
          if (mIdx < 0 || mIdx >= party.getMembers().size()) {
            self.reply("隊員編號超出範圍 (0 ~ " + (party.getMembers().size() - 1) + ")！");
            return;
          }
          PartyMember m = party.getMembers().get(mIdx);
          RowPosition next;
          if (parts.length > 2) {
            next = parts[2].equalsIgnoreCase("front") || parts[2].equals("前衛") || parts[2].equals("前")
                ? RowPosition.FRONT : RowPosition.BACK;
          } else {
            next = (m.getRow() == RowPosition.FRONT) ? RowPosition.BACK : RowPosition.FRONT;
          }
          m.setRow(next);
          String rowName = (next == RowPosition.FRONT) ? "前衛" : "後衛";
          self.reply("【站位變更】已將隊員「" + m.getName() + "」的戰鬥站位切換為【" + rowName + "】！");
          broadcastDrpgState(self);
        } catch (NumberFormatException e) {
          self.reply("隊員編號格式錯誤，請輸入數字 (0~4)。");
        }
      }
      case "tactics", "gambit", "ai", "戰術" -> {
        handleTactics(self, party, parts);
      }
      case "stat", "stats", "point", "points", "屬性" -> {
        handleStatPoints(self, party, parts);
      }
      default -> {
        self.reply("【小隊編制指令】\n"
            + "  party                    - 檢視 5 人小隊成員、血量、真元、道心(SAN)與陣位加成\n"
            + "  party recruit <名/ID>    - 邀請同道知己加入問道旅團 (上限 " + Party.MAX_PARTY_SIZE + " 人)\n"
            + "  party dismiss <名/ID>    - 請離隊員返回客棧安歇\n"
            + "  party switch <idx>       - 切換隊員前後排站位\n"
            + "  party stat [add <屬性>]  - 檢視主角修為等級與自由加點 (str/con/dex/int/wis)\n"
            + "  party enable <idx> <武學> - 設定隊員主修武學套路\n"
            + "  party tactics <idx>      - 檢視與自訂隊員戰術方針規則鏈 (Gambit)\n"
            + "  party energy <數值>       - 注入陣法靈威值 (測試大招專用)");
      }
    }
  }

  private void handleRecruit(Player self, Party party, String target) {
    partyService.recruitCompanionForPlayer(self, target);
  }

  private void handleDismiss(Player self, Party party, String target) {
    partyService.dismissCompanionForPlayer(self, target);
  }

  private void handleTactics(Player self, Party party, String[] parts) {
    if (parts.length < 2) {
      self.reply("【戰術方針 (Gambit System) 指令指引】\n"
          + "  party tactics <隊員編號: 0~" + (party.getMembers().size() - 1) + ">             - 檢視該隊員的戰術方針清單\n"
          + "  party tactics <隊員編號> reset           - 重置為職業預設戰術方針\n"
          + "  party tactics <隊員編號> clear           - 清空所有戰術方針\n"
          + "  party tactics <隊員編號> add <優先級> <條件> <數值> <目標> <技能ID> - 新增或覆蓋戰術方針\n"
          + "\n【條件 Condition 可選】:\n"
          + "  ALLY_HP_LESS_THAN (隊友氣血<%), SELF_HP_LESS_THAN (自身氣血<%),\n"
          + "  ENEMY_COUNT_GTE (敵方存活>=體), ENEMY_IS_BOSS (遭遇首領), RESOURCE_GTE (資源>=點), ALWAYS (無條件)\n"
          + "【目標 Target 可選】:\n"
          + "  LOWEST_HP_ALLY (氣血最低隊友), SELF (自身), CURRENT_ENEMY (當前集火目標), ALL_ENEMIES (全體敵怪), ALL_ALLIES (全體隊友)\n"
          + "【範例】:\n"
          + "  party tactics 1 add 1 ALLY_HP_LESS_THAN 60 LOWEST_HP_ALLY divine_healing\n"
          + "  party tactics 2 add 1 ALLY_HP_LESS_THAN 35 LOWEST_HP_ALLY divine_healing");
      return;
    }

    int mIdx;
    try {
      mIdx = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      self.reply("隊員編號格式錯誤，請輸入數字 (0~" + (party.getMembers().size() - 1) + ")。");
      return;
    }

    if (mIdx < 0 || mIdx >= party.getMembers().size()) {
      self.reply("隊員編號超出範圍 (0 ~ " + (party.getMembers().size() - 1) + ")！");
      return;
    }

    PartyMember m = party.getMembers().get(mIdx);

    // party tactics <idx>
    if (parts.length == 2) {
      if (m.getTactics().isEmpty()) {
        m.initDefaultTactics();
      }
      StringBuilder sb = new StringBuilder();
      sb.append("📋【戰術方針設定】隊員「").append(m.getName()).append("」(").append(m.getEffectiveClassName()).append(") 的戰術規則鏈：\n");
      for (TacticsRule rule : m.getTactics()) {
        String skillName = rule.getSkillId();
        var skOpt = templateReader.findSkill(rule.getSkillId());
        if (skOpt.isPresent()) {
          skillName = skOpt.get().getName();
        } else if (m.getSkills() != null) {
          skillName = m.getSkills().stream()
              .filter(s -> s.getId().equalsIgnoreCase(rule.getSkillId()))
              .map(PartyMemberSkill::getName)
              .findFirst()
              .orElse(rule.getSkillId());
        }
        sb.append("  ").append(rule.formatDescription(skillName)).append("\n");
      }
      sb.append("(戰術規則將依照優先級 #1, #2, #3... 依序判定，首個滿足條件者將立即觸發施展)");
      self.reply(sb.toString());
      return;
    }

    String sub = parts[2].toLowerCase();
    switch (sub) {
      case "reset" -> {
        m.resetTactics();
        self.reply("【戰術重置】已將隊員「" + m.getName() + "」的戰術方針重置為職業預設規則。");
        broadcastDrpgState(self);
      }
      case "clear" -> {
        m.clearTactics();
        self.reply("【戰術清空】已清空隊員「" + m.getName() + "」的所有戰術方針。");
        broadcastDrpgState(self);
      }
      case "add" -> {
        if (parts.length < 8) {
          self.reply("用法: party tactics " + mIdx + " add <優先級> <條件> <數值> <目標> <技能ID>\n"
              + "例如: party tactics " + mIdx + " add 1 ALLY_HP_LESS_THAN 60 LOWEST_HP_ALLY divine_healing");
          return;
        }
        try {
          int priority = Integer.parseInt(parts[3]);
          TacticsCondition condition = TacticsCondition.valueOf(parts[4].toUpperCase());
          int condVal = Integer.parseInt(parts[5]);
          TacticsTarget target = TacticsTarget.valueOf(parts[6].toUpperCase());
          String skillId = parts[7].trim();

          String skillName = skillId;
          var skOpt = templateReader.findSkill(skillId);
          if (skOpt.isPresent()) {
            skillName = skOpt.get().getName();
          } else if (m.getSkills() != null) {
            skillName = m.getSkills().stream()
                .filter(s -> s.getId().equalsIgnoreCase(skillId))
                .map(PartyMemberSkill::getName)
                .findFirst()
                .orElse(skillId);
          }

          m.getTactics().removeIf(r -> r.getPriority() == priority);

          TacticsRule newRule = TacticsRule.builder()
              .priority(priority)
              .condition(condition)
              .conditionValue(condVal)
              .target(target)
              .skillId(skillId)
              .enabled(true)
              .build();
          m.addTacticsRule(newRule);

          self.reply("【戰術設定成功】已更新隊員「" + m.getName() + "」的戰術方針：\n  " + newRule.formatDescription(skillName));
          broadcastDrpgState(self);
        } catch (NumberFormatException e) {
          self.reply("優先級或條件數值必須為整數！");
        } catch (IllegalArgumentException e) {
          self.reply("條件或目標名稱錯誤！請確認拼寫是否正確。\n條件: ALLY_HP_LESS_THAN, SELF_HP_LESS_THAN, ENEMY_COUNT_GTE, ENEMY_IS_BOSS, RESOURCE_GTE, ALWAYS\n目標: LOWEST_HP_ALLY, SELF, CURRENT_ENEMY, ALL_ENEMIES, ALL_ALLIES");
        }
      }
      case "delete", "remove", "del" -> {
        if (parts.length < 4) {
          self.reply("用法: party tactics " + mIdx + " delete <優先級序號>");
          return;
        }
        try {
          int priority = Integer.parseInt(parts[3]);
          boolean removed = m.getTactics().removeIf(r -> r.getPriority() == priority);
          if (removed) {
            self.reply("【戰術移除】已移除隊員「" + m.getName() + "」的第 #" + priority + " 條戰術方針。");
            broadcastDrpgState(self);
          } else {
            self.reply("找不到第 #" + priority + " 條戰術方針！");
          }
        } catch (NumberFormatException e) {
          self.reply("優先級序號必須為整數！");
        }
      }
      case "toggle" -> {
        if (parts.length < 4) {
          self.reply("用法: party tactics " + mIdx + " toggle <優先級序號>");
          return;
        }
        try {
          int priority = Integer.parseInt(parts[3]);
          var ruleOpt = m.getTactics().stream().filter(r -> r.getPriority() == priority).findFirst();
          if (ruleOpt.isPresent()) {
            TacticsRule rule = ruleOpt.get();
            rule.setEnabled(!rule.isEnabled());
            self.reply("【戰術切換】已將隊員「" + m.getName() + "」的第 #" + priority + " 條戰術方針設為【" + (rule.isEnabled() ? "啟用" : "停用") + "】。");
            broadcastDrpgState(self);
          } else {
            self.reply("找不到第 #" + priority + " 條戰術方針！");
          }
        } catch (NumberFormatException e) {
          self.reply("優先級序號必須為整數！");
        }
      }
      default -> {
        self.reply("未知的戰術指令「" + sub + "」。可用子指令: reset, clear, add, delete, toggle");
      }
    }
  }

  private void handleStatPoints(Player self, Party party, String[] parts) {
    PartyMember leader = party.getLeader();
    if (leader == null || leader.getStats() == null) {
      self.reply("未能尋得隊長資料！");
      return;
    }
    LivingStats stats = leader.getStats();

    if (parts.length < 2 || parts[1].equalsIgnoreCase("list") || parts[1].equalsIgnoreCase("view")) {
      StringBuilder sb = new StringBuilder();
      sb.append("═════════════【道途修為・屬性點數】═════════════\n");
      sb.append("  角色：").append(leader.getName()).append(" (Lv.").append(stats.getLevel()).append(")\n");
      sb.append("  當前修為：[").append(stats.getExp()).append(" / ").append(stats.getNextLevelExp()).append(" EXP]\n");
      sb.append("  未分配自由點數：【").append(stats.getFreeStatPoints()).append("】點\n");
      sb.append("──────────────────────────────────────────────\n");
      sb.append("  [STR] 力量/臂力：").append(stats.getStr()).append(" 點 (影響物理傷害、穿透與負重)\n");
      sb.append("  [CON] 根骨/體質：").append(stats.getCon()).append(" 點 (影響氣血上限 HP: ").append(stats.getMaxHp()).append("、減傷)\n");
      sb.append("  [DEX] 靈巧/身法：").append(stats.getDex()).append(" 點 (影響暴擊、命中、閃避迴避)\n");
      sb.append("  [INT] 悟性/智力：").append(stats.getIntelligence()).append(" 點 (影響真元上限 MP: ").append(stats.getMaxMp()).append("、法術傷害)\n");
      sb.append("  [WIS] 定力/精神：").append(stats.getWis()).append(" 點 (影響治療效果、法力恢復、道心抗性)\n");
      sb.append("──────────────────────────────────────────────\n");
      sb.append("  自由配點指令：party stat add <str|con|dex|int|wis> [點數]\n");
      sb.append("══════════════════════════════════════════════");
      self.reply(sb.toString());
      return;
    }

    if (parts[1].equalsIgnoreCase("add") || parts[1].equalsIgnoreCase("allocate") || parts[1].equalsIgnoreCase("加點")) {
      if (parts.length < 3) {
        self.reply("用法: party stat add <str|con|dex|int|wis> [點數]");
        return;
      }
      String statName = parts[2];
      int amount = 1;
      if (parts.length >= 4) {
        try {
          amount = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
          self.reply("請輸入正確的點數，例如: party stat add str 2");
          return;
        }
      }
      if (amount <= 0) {
        self.reply("加點數值必須大於 0！");
        return;
      }
      if (stats.getFreeStatPoints() < amount) {
        self.reply("【自由點數不足】當前僅有 " + stats.getFreeStatPoints() + " 點可用！");
        return;
      }

      boolean success = getXpProgressionService().allocateStatPoint(leader, statName, amount);
      if (success) {
        // 同步 Player 實體屬性
        if (self.getStats() != null) {
          self.getStats().setStr(stats.getStr());
          self.getStats().setCon(stats.getCon());
          self.getStats().setDex(stats.getDex());
          self.getStats().setIntelligence(stats.getIntelligence());
          self.getStats().setWis(stats.getWis());
          self.getStats().setHp(stats.getHp());
          self.getStats().setMaxHp(stats.getMaxHp());
          self.getStats().setMp(stats.getMp());
          self.getStats().setMaxMp(stats.getMaxMp());
          self.getStats().setFreeStatPoints(stats.getFreeStatPoints());
        }
        self.reply("【道胎拓寬】成功將 " + amount + " 點自由點數注入【" + statName.toUpperCase() + "】！剩餘點數：" + stats.getFreeStatPoints() + " 點。");
        broadcastDrpgState(self);
      } else {
        self.reply("無效的屬性名稱，可選屬性：str (力量), con (根骨), dex (身法), int (悟性), wis (定力)");
      }
    }
  }

  private void broadcastDrpgState(Player player) {
    if (player == null) return;
    broadcastService.broadcastState(player);
  }

  @Override
  public String getDescription() {
    return "檢視或管理 5 人冒險者隊伍 (Party)";
  }
}
