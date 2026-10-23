package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.combat.DrpgBattleService;
import com.example.htmlmud.combat.CombatService;
import com.example.htmlmud.combat.TargetSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("k")
public class KillCommand implements PlayerCommand {

  private final TargetSelector targetSelector; // 注入工具
  private final DrpgBattleService drpgBattleService;
  private final CombatService combatService;

  public KillCommand(TargetSelector targetSelector, DrpgBattleService drpgBattleService, CombatService combatService) {
    super();
    this.targetSelector = targetSelector;
    this.drpgBattleService = drpgBattleService;
    this.combatService = combatService;
  }

  @Override
  public String getKey() {
    return "kill";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    if (args.isBlank()) {
      player.reply("你要攻擊誰？");
      return;
    }

    // 1. 取得房間內的怪物列表
    Room room = player.getCurrentRoom();
    if (room != null && room.hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag.SAFE_ZONE)) {
      player.reply("此處乃安全祥和之地，嚴禁動武！");
      return;
    }

    // 2. 交給 Selector 處理目標字串
    Mob target = targetSelector.selectMob(room != null ? room.getMobs() : java.util.List.of(), args);
    if (target == null) {
      player.reply("這裡沒有看到 '" + args + "'。");
      return;
    }

    // 3. 全面交由小隊戰鬥引擎接管 (SP-08: 5人小隊全員協同迎敵)
    if (drpgBattleService != null) {
      boolean started = drpgBattleService.startFieldBattle(player, target);
      if (started) {
        return;
      }
    }

    // 防禦性 fallback：相容歷史單挑通道
    combatService.startCombat(player, target.getId());
  }
}
