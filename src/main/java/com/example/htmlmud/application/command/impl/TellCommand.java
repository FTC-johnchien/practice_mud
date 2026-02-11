package com.example.htmlmud.application.command.impl;

import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.application.command.annotation.CommandAlias;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.service.WorldManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@CommandAlias({"tell", "whisper", "t"})
@RequiredArgsConstructor
public class TellCommand implements PlayerCommand {

  private final WorldManager worldManager;

  @Override
  public String getKey() {
    return "tell";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    // 1. 參數檢查
    // 格式: tell <player> <message>
    if (args == null || args.isBlank()) {
      throw new MudException("你想對誰說什麼？ (格式: tell <名字> <內容>)");
    }

    String[] parts = args.split(" ", 2);
    if (parts.length < 2) {
      throw new MudException("你還沒說內容呢。");
    }

    String targetName = parts[0];
    String content = parts[1];

    // 2. 尋找目標 (假設 WorldManager 有用 Map 或 Cache 存線上玩家)
    // 這裡我們直接拋出異常，讓 Dispatcher 處理錯誤訊息
    Player target = worldManager.findPlayerByName(targetName)
        .orElseThrow(() -> new MudException("在這個世界上找不到叫 '" + targetName + "' 的人。"));

    // 3. 檢查是不是對自己說話
    if (target.getId().equals(player.getId())) {
      throw new MudException("你自言自語，覺得有些空虛。");
    }

    // 4. 發送訊息 (Tell)
    // 給對方看
    target.reply(String.format("%s 悄悄地對你說: %s", player.getName(), content));

    // 給自己看 (確認發送成功)
    player.reply(String.format("你對 %s 說: %s", target.getName(), content));
  }
}
