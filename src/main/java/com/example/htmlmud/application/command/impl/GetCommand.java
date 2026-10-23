package com.example.htmlmud.application.command.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyItemSlot;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.domain.world.GameStateBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@CommandAlias({"get", "loot", "open"})
@RequiredArgsConstructor
public class GetCommand implements PlayerCommand {

  public static final int PERSONAL_INVENTORY_CAPACITY = 12;

  private final PartyService partyService;
  private final GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "get";
  }

  @Override
  public void execute(String args) {
    Player player = MudContext.currentPlayer();

    // 基本檢查
    if (args == null || args.isBlank()) {
      player.reply("$N要拾取或搜刮什麼？");
      return;
    }

    Room room = player.getCurrentRoom();
    if (room == null) {
      player.reply("你不在任何房間中。");
      return;
    }

    Optional<GameItem> opt = room.tryPickItem(args.trim(), player);
    if (opt.isPresent()) {
      GameItem pickItem = opt.get();
      Party party = partyService.getOrCreateParty(player.getName());

      // 1. 戰利品容器 (儲物袋 / 寶箱) 或帶有掉落物的容器實體 -> 一鍵搜刮進入小隊倉庫 (UNIFY-01)
      boolean isContainer = pickItem.getType() == ItemType.CONTAINER ||
          (pickItem.getContents() != null && !pickItem.getContents().isEmpty());

      if (isContainer) {
        List<GameItem> contents = new ArrayList<>(pickItem.getContents());
        if (contents.isEmpty()) {
          player.reply("【" + pickItem.getName() + "】裡面空空如也，什麼也沒有。");
        } else {
          List<String> lootedNames = new ArrayList<>();
          for (GameItem inner : contents) {
            if (party != null && party.getInventory() != null) {
              party.getInventory().addGameItem(inner);
            }
            lootedNames.add(inner.getDisplayName() + (inner.getAmount() > 1 ? " x" + inner.getAmount() : ""));
          }
          String lootList = String.join("、", lootedNames);
          player.reply("\u001B[1;32m👝 你打開了 " + pickItem.getName() + "，搜刮獲得了：\u001B[1;33m" + lootList + "\u001B[1;32m！已全數收入【小隊儲物倉庫】。\u001B[0m");
        }
        // 容器已完成搜刮，即刻消散，不存入背包
      } else if (pickItem.getType() == ItemType.CORPSE) {
        // 普通死屍禁止塞入背包
        player.reply("你仔細翻檢了這具殘骸，裡面已無任何可用之物，殘骸隨風化作塵土消散。");
        // 殘骸不入背包，自然消散
      } else {
        // 2. 一般實體物品拾取 (全面直接存入小隊道途行囊 - 大一統單一真相源)
        if (party != null && party.getInventory() != null && party.getInventory().addGameItem(pickItem)) {
          player.reply("📦 你拾起了 " + pickItem.getDisplayName() + "，已妥善收入【小隊行囊】！");
        } else if (party == null || party.getInventory() == null) {
          // 兜底防禦：若無小隊實體則存入 player 隨身背包
          player.getInventory().add(pickItem);
          player.reply("📦 你拾起了 " + pickItem.getDisplayName() + "，已妥善收入行囊！");
        } else {
          room.dropItem(pickItem);
          player.reply("⚠️ 小隊行囊已滿載（上限 " + party.getInventory().getCapacity() + " 格），無法拾取 " + pickItem.getDisplayName() + "！");
          return;
        }
      }

      // 即時向全隊/玩家推播最新狀態 (地面物品即時移除)
      broadcastService.broadcastState(player);
    } else {
      player.reply("這裡沒有 '" + args + "'。");
    }
  }
}
