package com.example.htmlmud.application.command.impl;

import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.GameItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias("get")
@RequiredArgsConstructor
public class GetCommand implements PlayerCommand {

  @Override
  public String getKey() {
    return "get";
  }

  @Override
  public void execute(Player self, String args) {
    // 基本檢查
    if (args.isEmpty()) {
      self.getService().getMessageUtil().send("$N要撿什麼？", self);
      return;
    }

    // 1. 準備 Future 接收結果
    CompletableFuture<GameItem> future = new CompletableFuture<>();

    // 2. 發送訊息給房間： "我要撿 'sword' (args)"
    // 注意：這裡我們還不知道房間到底有沒有劍，只傳字串
    Room room = self.getCurrentRoom();

    // 3. 等待結果 (Virtual Thread 不會卡死)
    try {
      room.tryPickItem(args, self, future);
      GameItem item = future.orTimeout(1, java.util.concurrent.TimeUnit.SECONDS).join();
      if (item != null) {



        // TODO 檢查背包是否已滿 或 荷重是否足夠



        self.getInventory().add(item);
        self.getService().getMessageUtil().send("$N撿起了 " + item.getDisplayName(), self);
      } else {
        self.reply("這裡沒有 '" + args + "'。");
      }
    } catch (Exception e) {
      log.error("GetCommand error", e);
      self.reply("發生錯誤:");
    }



    // null actor.reply("這裡沒有看到 '" + args + "'。");

    // 房間通知
    // picker.reply("你撿起了 " + targetItem.getDisplayName());
    // doBroadcastToOthers(roomActor, picker.getId(),
    // picker.getName() + " 撿起了 " + targetItem.getDisplayName());

  }
}
