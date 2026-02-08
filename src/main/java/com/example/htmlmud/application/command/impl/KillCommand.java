package com.example.htmlmud.application.command.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.service.CombatService;
import com.example.htmlmud.infra.util.AnsiColor;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.infra.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("k")
@RequiredArgsConstructor
public class KillCommand implements PlayerCommand {

  private final MessageUtil messageUtil;

  private final TargetSelector targetSelector; // 注入工具

  private final CombatService combatService;

  @Override
  public String getKey() {
    return "kill";
  }

  @Override
  public void execute(Player self, String args) {
    if (args.isBlank()) {
      self.reply("$N要攻擊誰？");
      return;
    }

    // 1. 取得房間內的怪物列表
    Room room = self.getCurrentRoom();

    // 2. 交給 Selector 處理複雜字串
    // args 可能是 "red goblin", "elite soldier 2"
    // TODO pvp的處理
    Mob target = targetSelector.selectMob(room.getMobs(), args);
    if (target == null) {
      self.reply("這裡沒有看到 '" + args + "'。");
      return;
    }
    // log.info("name:{} defense: {}", target.getTemplate().name(), target.defense);

    // 3. 執行戰鬥邏輯
    combatService.startCombat(self, target);
    // log.info("name:{} {}", self.getName(), self.getNickname());

    // 訊息處理
    String messageTemplate = ColorText.wrap(AnsiColor.RED, "$N對著$n喝道﹕「臭賊﹗今日不是你死就是我活﹗」");
    room.broadcast(self.getId(), target.getId(), messageTemplate);
    // for (Living receiver : room.getPlayers()) {
    // messageUtil.send(messageTemplate, self, target, receiver);
    // }
  }
}
