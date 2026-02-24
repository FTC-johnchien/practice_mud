package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.service.CombatService;
import com.example.htmlmud.domain.service.TargetSelector;
import com.example.htmlmud.protocol.util.AnsiColor;
import com.example.htmlmud.protocol.util.ColorText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("k")
@RequiredArgsConstructor
public class KillCommand implements PlayerCommand {

  private final TargetSelector targetSelector; // 注入工具

  private final CombatService combatService;

  @Override
  public String getKey() {
    return "kill";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    if (args.isBlank()) {
      player.reply("$N要攻擊誰？");
      return;
    }

    // 1. 取得房間內的怪物列表
    Room room = player.getCurrentRoom();

    // 2. 交給 Selector 處理複雜字串
    // args 可能是 "red goblin", "elite soldier 2"
    // TODO pvp的處理
    Mob target = targetSelector.selectMob(room.getMobs(), args);
    if (target == null) {
      player.reply("這裡沒有看到 '" + args + "'。");
      return;
    }
    // log.info("name:{} defense: {}", target.getTemplate().name(), target.defense);

    // 發起戰鬥
    combatService.startCombat(player, target.getId());

    // 【節奏控制】
    // 攻擊者：立即獲得攻擊機會 (或是很短的延遲)
    player.nextAttackTime = System.currentTimeMillis();

    // 被攻擊對象接收到被攻擊事件
    target.onAttacked(player.getId());

    // log.info("name:{} {}", self.getName(), self.getNickname());

    // 訊息處理
    String messageTemplate = ColorText.wrap(AnsiColor.RED, "$N對著$n喝道﹕「臭賊﹗今日不是你死就是我活﹗」");
    room.broadcast(player.getId(), target.getId(), messageTemplate);
    // for (Living receiver : room.getPlayers()) {
    // MessageUtil.send(messageTemplate, self, target, receiver);
    // }
  }
}
