package com.example.htmlmud.application.command.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import com.example.htmlmud.infra.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("k")
@RequiredArgsConstructor
public class KillCommand implements PlayerCommand {

  private final MessageUtil messageUtil;

  private final WorldManager worldManager;

  private final TargetSelector targetSelector; // 注入工具

  @Override
  public String getKey() {
    return "kill";
  }

  @Override
  public void execute(PlayerActor self, String args) {
    if (args.isBlank()) {
      self.reply("你要攻擊誰？");
      return;
    }

    // 1. 取得房間內的怪物列表
    RoomActor room = worldManager.getRoomActor(self.getCurrentRoomId());
    // 這裡假設 room 有 getMobsSnapshot() 回傳 List<MobActor>
    // 注意：為了線程安全，這裡最好是 Snapshot 或是能確保讀取安全的列表
    List<MobActor> mobsInRoom = room.getMobs();

    // 2. 交給 Selector 處理複雜字串
    // args 可能是 "red goblin", "elite soldier 2"
    // TODO pvp的處理
    MobActor target = targetSelector.selectMob(mobsInRoom, args);
    if (target == null) {
      self.reply("這裡沒有看到 '" + args + "'。");
      return;
    }
    log.info("name:{} defense: {}", target.getTemplate().name(), target.getState().defense);

    // 3. 執行戰鬥邏輯
    self.getState().combatTargetId = target.getId();
    self.getState().isInCombat = true;
    target.onAttacked(self.getId());
    // log.info("name:{} {}", self.getName(), self.getNickname());

    // 訊息處理
    String messageTemplate = "$N對$n大喊受死吧 一邊擺出了戰鬥架式！";
    List<LivingActor> audiences = new ArrayList<>();
    audiences.addAll(room.getPlayers());
    for (LivingActor receiver : audiences) {
      messageUtil.send(messageTemplate, self, target, receiver);
    }
    // self.reply("你對 " + target.getTemplate().name() + " 大喊受死吧 一邊擺出了戰鬥架式！");
    room.broadcast(
        "log:KillCommand 目前 " + self.getState().hp + " / " + self.getState().maxHp + " HP");
  }
}
