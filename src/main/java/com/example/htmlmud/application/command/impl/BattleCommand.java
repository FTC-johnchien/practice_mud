package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias({"fight", "bt"})
public class BattleCommand implements PlayerCommand {

  private final DrpgBattleService battleService;
  private final DungeonManager dungeonManager;

  public BattleCommand(DrpgBattleService battleService, DungeonManager dungeonManager) {
    super();
    this.battleService = battleService;
    this.dungeonManager = dungeonManager;
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.config.GameConfig gameConfig;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private org.springframework.core.env.Environment environment;

  public void setGameConfig(com.example.htmlmud.config.GameConfig gameConfig) {
    this.gameConfig = gameConfig;
  }

  public void setEnvironment(org.springframework.core.env.Environment environment) {
    this.environment = environment;
  }

  private boolean isDebugAllowed() {
    if (gameConfig != null && gameConfig.getDebug() != null && gameConfig.getDebug().isEnabled()) {
      return true;
    }
    if (environment != null && (environment.matchesProfiles("dev") || environment.matchesProfiles("test"))) {
      return true;
    }
    return false;
  }

  @Override
  public String getKey() {
    return "battle";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim().toLowerCase() : "";
    DungeonPosition pos = dungeonManager.getPlayerPosition(self.getName());

    if (input.startsWith("target ") || input.startsWith("t ")) {
      String[] parts = input.split("\\s+");
      if (parts.length >= 2) {
        try {
          int idx = Integer.parseInt(parts[1]);
          battleService.selectTarget(self, idx, pos);
        } catch (NumberFormatException e) {
          self.reply("請輸入有效的目標編號！");
        }
      }
    } else if (input.equals("flee") || input.equals("escape") || input.equals("run") || input.equals("遁地")) {
      battleService.flee(self, pos);
    } else if (input.equals("ult") || input.equals("cast") || input.equals("奧義")) {
      battleService.castPartyUltimate(self, pos);
    } else if (input.startsWith("combo ") || input.startsWith("合擊 ")) {
      String comboId = input.replaceFirst("^(combo|合擊)\\s+", "").trim();
      battleService.castComboSkill(self, comboId, pos);
    } else if (input.equals("test") || input.equals("dummy") || input.equals("木樁")) {
      if (!isDebugAllowed()) {
        self.reply("【仙道戒律】玄鐵試道傀儡演武僅於除錯環境開放，當前已受天道法則封禁。");
        return;
      }
      battleService.startTrainingBattle(self, pos);
    } else if (input.equals("charge") || input.equals("充能")) {
      if (!isDebugAllowed()) {
        self.reply("【仙道戒律】陣法靈威充能法門僅於除錯環境開放，當前已受天道法則封禁。");
        return;
      }
      battleService.chargeCombatResources(self, pos);
    } else if (input.equals("fight") || input.equals("attack") || input.equals("迎戰") || input.isEmpty()) {
      battleService.fight(self, pos);
    } else {
      String debugSection = isDebugAllowed()
          ? "  battle test / 木樁        - 開啟玄鐵試道傀儡測試戰鬥 (除錯模式專用)\n"
              + "  battle charge / 充能      - 陣法靈威充滿 100 點 (除錯模式專用)\n"
          : "";
      self.reply("【戰鬥指令】\n"
          + "  battle fight / 迎戰       - 全隊凝神迎戰，拔劍直攻目標\n"
          + debugSection
          + "  battle target <編號>     - 切換集火目標 (0: 前排一號, 1: 前排二號...)\n"
          + "  battle flee / 遁地       - 施展遁地金光脫離戰場 (-5 SAN)\n"
          + "  battle ult / 奧義        - 釋放陣法終極奧義 (需靈威 100)");
    }
  }

  @Override
  public String getDescription() {
    return "太陰地宮小隊戰鬥指令 (支援 fight/迎戰, flee/遁地, ult, target)";
  }
}