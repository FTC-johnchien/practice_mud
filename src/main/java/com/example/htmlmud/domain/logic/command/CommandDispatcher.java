package com.example.htmlmud.domain.logic.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.logic.command.annotation.CommandAlias;
import com.example.htmlmud.domain.logic.command.impl.MoveCommand;
import com.example.htmlmud.domain.model.Direction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommandDispatcher {

  // 指令註冊表: "look" -> LookCommand Object
  private final Map<String, PlayerCommand> commandMap = new HashMap<>();

  // Spring 會自動注入所有實作 PlayerCommand 的 Bean (LookCommand, MoveCommand...)
  public CommandDispatcher(List<PlayerCommand> commands) {
    for (PlayerCommand cmd : commands) {
      // 1. 註冊主鍵 (例如 "look", "move")
      register(cmd.getKey(), cmd);

      // 2. 【核心修改】檢查是否有 Alias 註解
      if (cmd.getClass().isAnnotationPresent(CommandAlias.class)) {
        CommandAlias annotation = cmd.getClass().getAnnotation(CommandAlias.class);

        // 3. 註冊所有別名
        for (String alias : annotation.value()) {
          register(alias, cmd);
        }
      }

      // 額外處理 MoveCommand 的方向縮寫 (因為這不是單純的別名，還涉及參數轉換)
      // 建議還是保留在程式碼裡特殊處理，或者用另一種 @DirectionAlias 處理
      registerDirectionAliases(commands);
    }
  }

  private void register(String key, PlayerCommand cmd) {
    commandMap.put(key.toLowerCase(), cmd);
  }

  // 處理方向鍵 (n, s, e, w)
  private void registerDirectionAliases(List<PlayerCommand> commands) {
    // 找到 MoveCommand
    PlayerCommand moveCmd =
        commands.stream().filter(c -> c.getKey().equals("move")).findFirst().orElse(null);

    if (moveCmd != null) {
      for (Direction d : Direction.values()) {
        register(d.getShortName(), moveCmd); // n, s, e, w
        register(d.getFullName(), moveCmd); // north, south...
      }
    }
  }

  /**
   * 核心派發邏輯
   *
   * @param input 玩家輸入的原始字串，例如 "look north" 或 "kill goblin"
   */
  public void dispatch(PlayerActor actor, String input) {
    if (input == null || input.isBlank())
      return;

    // 1. 切割字串： "kill goblin" -> key="kill", args="goblin"
    String[] parts = input.trim().split("\\s+", 2); // 只切成兩份
    String key = parts[0].toLowerCase();
    String args = parts.length > 1 ? parts[1] : "";

    log.info("key:{}, args:{}", key, args);
    // 2. 查找指令
    PlayerCommand command = commandMap.get(key);

    if (command != null) {
      // 【關鍵修正】
      // 如果玩家輸入的是 "n"，我們需要把 "n" 當作參數傳給 MoveCommand
      // 或者是 MoveCommand 內部 logic 要知道 "n" 等於 "move n"
      if (command instanceof MoveCommand && Direction.parse(key) != null) {
        // 如果指令本身就是方向 (例如輸入 "n")，把 key 當作 args 傳進去
        command.execute(actor, key);
      } else {
        // 否則正常執行 (例如 "move north")
        command.execute(actor, args);
      }
    } else {
      // 4. 找不到指令的預設處理
      actor.reply("我不懂 '" + key + "' 是什麼意思。輸入 'help' 查看指令列表。");
    }
  }
}
