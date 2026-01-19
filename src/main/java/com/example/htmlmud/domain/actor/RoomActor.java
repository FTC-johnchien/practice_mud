package com.example.htmlmud.domain.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.example.htmlmud.domain.actor.core.VirtualActor; // 引用您的基礎類別
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.RoomStateRecord;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.protocol.RoomMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 1. 繼承 VirtualActor，並指定泛型為 RoomMessage
public class RoomActor extends VirtualActor<RoomMessage> {

  @Getter
  private final RoomTemplate template;

  // 房間內的玩家 (Runtime State)
  @Getter
  private final Set<PlayerActor> players = ConcurrentHashMap.newKeySet();

  @Getter
  private final Set<MobActor> mobs = ConcurrentHashMap.newKeySet();

  private final List<GameItem> items = new ArrayList<>(); // 地上的物品

  public RoomActor(RoomTemplate template, List<GameItem> initialItems) {
    // 2. 傳入 Actor 名稱給父類別 (方便 Log 排查)
    super("room-" + template.id());
    this.template = template;

    if (initialItems != null) {
      this.items.addAll(initialItems);
    }

    // 3. 啟動 Actor (這會呼叫父類別的 start -> runLoop)
    this.start();
  }

  // --- 實作父類別的抽象方法 ---

  @Override
  protected void handleMessage(RoomMessage msg) {
    // 這裡的邏輯跟之前一模一樣，但不需要自己寫 loop 和 try-catch 了
    switch (msg) {
      case RoomMessage.PlayerEnter(var player, var future) -> {
        log.info("Player {} entered room", player.getId());
        players.add(player);
        broadcastToOthers(player.getId(), "看到 " + player.getDisplayName() + " 走了進來。");
        log.debug("Player {} entered room {}", player.getDisplayName(), template.id());
        if (future != null)
          future.complete(null);
      }

      case RoomMessage.PlayerLeave(var playerId) -> {
        players.stream().filter(p -> p.getId().equals(playerId)).findFirst().ifPresent(p -> {
          if (players.remove(p))
            broadcastToOthers(playerId, p.getDisplayName() + " 離開了。");
        });
      }

      case RoomMessage.Look(var playerId, var future) -> {
        log.info("Player {} Look room", playerId);
        StringBuilder sb = new StringBuilder();
        sb.append("\u001B[1;36m").append(template.name()).append("\u001B[0m\r\n");
        sb.append(template.description()).append("\r\n");

        if (template.exits() != null && !template.exits().isEmpty()) {
          sb.append("\u001B[33m[出口]: ").append(String.join(", ", template.exits().keySet()))
              .append("\u001B[0m\r\n");
        }

        StringBuilder others = new StringBuilder();
        players.stream().filter(p -> !p.getId().equals(playerId))
            .forEach(p -> others.append(p.getDisplayName()).append(" "));

        if (!others.isEmpty()) {
          sb.append("\u001B[35m[這裡有]: \u001B[0m").append(others).append("\r\n");
        }
        future.complete(sb.toString());
      }

      case RoomMessage.Say(var sourceId, var content) -> {
        PlayerActor speaker =
            players.stream().filter(p -> p.getId().equals(sourceId)).findFirst().orElse(null);
        String name = (speaker != null) ? speaker.getDisplayName() : "有人";
        broadcast(name + ": " + content);
      }
      case RoomMessage.TryPickItem(var itemId, var picker) -> {
        // 【關鍵併發控制】
        // var item = items.stream().filter(i -> i.id.equals(itemId)).findFirst();
        // if (item.isPresent()) {
        // items.remove(item.get());
        // 回覆給 Command: 成功
        // picker.send(new ActorMessage("...", new InternalCommand.PickSuccess(item.get())));
        // } else {
        // 回覆給 Command: 失敗
        // picker.send(new ActorMessage("...", new InternalCommand.PickFail("東西不在了")));
        // }
      }
    }
  }

  // --- 物品操作邏輯 ---

  public void dropItem(GameItem item) {
    items.add(item);
    // 標記為 Dirty (需要存檔)
    // WorldManager.markDirty(this.template.id());
  }

  // public Optional<Item> pickItem(String keyword) {
  // 簡單搜尋邏輯
  // var found = items.stream().filter(i -> isMatch(i, keyword)) // 需實作 isMatch
  // .findFirst();

  // found.ifPresent(items::remove);
  // return found; // 這裡回傳後，記得要標記 Dirty
  // }

  // --- 輔助方法 (保持不變) ---

  private void broadcast(String message) {
    players.forEach(p -> p.sendText(message));
  }

  private void broadcastToOthers(String sourceId, String message) {
    players.stream().filter(p -> !p.getId().equals(sourceId)).forEach(p -> p.sendText(message));
  }

  // 產生快照 (只存變動的部分)
  public RoomStateRecord toRecord() {
    return new RoomStateRecord(template.id(), new ArrayList<>(items));
  }
}
