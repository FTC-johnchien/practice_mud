package com.example.htmlmud.application.command.impl;

import java.util.List;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.application.command.parser.TargetSelector;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.actor.impl.RoomActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("k")
@RequiredArgsConstructor
public class KillCommand implements PlayerCommand {

  private final WorldManager worldManager;

  private final TargetSelector targetSelector; // 注入工具

  @Override
  public String getKey() {
    return "kill";
  }

  @Override
  public void execute(PlayerActor actor, String args) {
    if (args.isBlank()) {
      actor.reply("你要攻擊誰？");
      return;
    }

    // 1. 取得房間內的怪物列表
    RoomActor room = worldManager.getRoomActor(actor.getCurrentRoomId());
    // 這裡假設 room 有 getMobsSnapshot() 回傳 List<MobActor>
    // 注意：為了線程安全，這裡最好是 Snapshot 或是能確保讀取安全的列表
    List<MobActor> mobsInRoom = room.getMobsSnapshot();

    // 2. 交給 Selector 處理複雜字串
    // args 可能是 "red goblin", "elite soldier 2"
    MobActor target = targetSelector.selectMob(mobsInRoom, args);
    log.info("name:{} defense: {}", target.getTemplate().name(), target.getState().defense);

    if (target == null) {
      actor.reply("這裡沒有看到 '" + args + "'。");
      return;
    }

    // 3. 執行戰鬥邏輯
    actor.getState().combatTargetId = target.getId();
    actor.getState().isInCombat = true;
    log.info("name:{} {}", actor.getName(), actor.getNickname());
    actor.reply("你對 " + target.getTemplate().name() + " 大喊受死吧 一邊擺出了戰鬥架式！");
    // actor.reply("你開始攻擊 " + target.getTemplate().name() + "！");
    target.onAttacked(actor, 0);
  }
}
