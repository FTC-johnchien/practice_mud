package com.example.htmlmud.application.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.impl.MoveCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.enums.Direction;
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

      // 2. 使用 Optional 處理別名註解：只進行一次反射查詢，且語法簡潔
      Optional.ofNullable(cmd.getClass().getAnnotation(CommandAlias.class)).map(CommandAlias::value)
          .ifPresent(aliases -> Arrays.stream(aliases).forEach(alias -> register(alias, cmd)));
    }

    // 3. 額外處理方向縮寫：移出迴圈，只需執行一次
    registerDirectionAliases(commands);
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
  public void dispatch(String input) {
    if (input == null || input.isBlank())
      return;

    // 1. 切割字串： "kill goblin" -> key="kill", args="goblin"
    String[] parts = input.trim().split("\\s+", 2); // 只切成兩份
    String key = parts[0].toLowerCase();
    String args = parts.length > 1 ? parts[1] : "";

    // log.info("key:{}, args:{}", key, args);
    Player player = MudContext.currentPlayer();

    // 2. 查找指令
    PlayerCommand command = commandMap.get(key);
    if (command == null) {
      // 找不到指令的預設處理
      player.reply("我不懂 '" + key + "' 是什麼意思。輸入 'help' 查看指令列表。");
      return;
    }

    try {
      // 如果玩家輸入的是 "n"，我們需要把 "n" 當作參數傳給 MoveCommand
      // 或者是 MoveCommand 內部 logic 要知道 "n" 等於 "move n"
      if (command instanceof MoveCommand && Direction.parse(key) != null) {
        // 如果指令本身就是方向 (例如輸入 "n")，把 key 當作 args 傳進去
        command.execute(key);
      } else {
        // 否則正常執行 (例如 "move north")
        command.execute(args);
      }
    } catch (MudException e) {
      // 情境 A：遊戲邏輯錯誤 (錢不夠、找不到人)
      // 直接把錯誤訊息 "Tell" 給玩家
      player.reply(e.getMessage());
    } catch (java.util.concurrent.CompletionException e) {
      // 如果底層是我們定義的 MudException，就拿出來顯示
      if (e.getCause() instanceof MudException mudEx) {
        player.reply(mudEx.getMessage());
      } else {
        // 否則就是真·系統錯誤
        log.error("Async Error", e);
        player.reply("操作逾時或發生錯誤。");
      }
    } catch (Exception e) {
      // 情境 B：系統未預期的錯誤 (Bug)
      // 1. 記錄詳細 Log 給工程師看
      log.error("Command execution error: user={}, cmd={}", player.getId(), input, e);

      // 2. 告訴玩家發生系統錯誤 (不要顯示 StackTrace)
      player.reply("發生未知的力量干擾了你的行動 (系統錯誤)。");

      // 3. 如果是 CompletableFuture 的封裝錯誤，嘗試解包
      if (e instanceof java.util.concurrent.CompletionException
          && e.getCause() instanceof MudException) {
        player.reply(e.getCause().getMessage());
      }
    }
  }
}
