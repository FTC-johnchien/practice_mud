package com.example.htmlmud.application.command.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.model.DungeonTile.TileType;
import com.example.htmlmud.domain.dungeon.model.StepResult;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"dg", "step", "map"})
public class DungeonCommand implements PlayerCommand {

  private static final List<String> TOMB_MOBS = List.of(
      "taiyin_tomb:corpse_doll",
      "taiyin_tomb:blood_python",
      "taiyin_tomb:bone_bat",
      "taiyin_tomb:tomb_guard"
  );

  private static final List<String> TOMB_ITEMS = List.of(
      "taiyin_tomb:taiyin_pill",
      "taiyin_tomb:purify_talisman",
      "taiyin_tomb:bronze_sword",
      "taiyin_tomb:yin_robe",
      "taiyin_tomb:ancient_relic",
      "taiyin_tomb:tomb_key"
  );

  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final PartyService partyService;
  private final com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;
  private final MoveCommand moveCommand;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;
  private final TemplateReader templateReader;

  @Override
  public String getKey() {
    return "dungeon";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String input = args != null ? args.trim().toLowerCase() : "";

    // 處理 enter / switch 指令：例如 "dungeon enter mozhu_mines_b1f" 或 "dungeon switch taiyin"
    if (input.startsWith("enter") || input.startsWith("switch")) {
      String[] parts = input.split("\\s+");
      String targetFloor = parts.length > 1 ? parts[1] : null;
      if (targetFloor == null || targetFloor.isBlank()) {
        String currentRoom = self.getCurrentRoomId();
        if (currentRoom != null && currentRoom.contains("taiyin")) {
          targetFloor = "taiyin_tomb_b1f";
        } else {
          targetFloor = "mozhu_mines_b1f";
        }
      } else if (targetFloor.equals("mozhu") || targetFloor.equals("mine")) {
        targetFloor = "mozhu_mines_b1f";
      } else if (targetFloor.equals("taiyin") || targetFloor.equals("tomb")) {
        targetFloor = "taiyin_tomb_b1f";
      }
      DungeonFloor target = dungeonManager.getFloor(targetFloor);
      if (target == null) {
        self.reply("【系統】找不到地牢代號：" + targetFloor + "，目前開放：" + String.join(", ", dungeonManager.getAllFloorIds()));
        return;
      }
      self.setInDungeon(true);
      DungeonPosition pos = dungeonManager.switchFloor(self.getName(), targetFloor);
      self.reply("\n\u001B[1;32m🌀【踏入迷宮】你已步入【" + target.getName() + "】！靈識雷達展開！\u001B[0m\n"
          + dungeonNavigator.inspectForward(target, pos));
      broadcastService.broadcastState(self);
      return;
    }

    // 處理 list 指令：例如 "dungeon list"
    if (input.equals("list")) {
      self.reply("【開放地牢列表】\n" + String.join("\n", dungeonManager.getAllFloorIds()));
      return;
    }

    // 若玩家處於城鎮模式中 (未進入地牢)：
    if (!self.isInDungeon()) {
      switch (input) {
        case "w", "step w" -> moveCommand.execute("north");
        case "s", "step s" -> moveCommand.execute("south");
        case "a", "step a" -> moveCommand.execute("west");
        case "d", "step d" -> moveCommand.execute("east");
        default -> {
          self.reply("【城鎮導航】使用 W/A/S/D 或點擊羅盤在城鎮中穿梭。");
          broadcastService.broadcastState(self);
        }
      }
      return;
    }

    // 取得當前位置或預設進入當前所在區域地牢
    DungeonPosition pos = dungeonManager.getOrCreatePosition(self.getName(), null);
    if (pos == null || pos.getFloorId() == null) {
      String defaultFloor = "mozhu_mines_b1f";
      String currentRoom = self.getCurrentRoomId();
      if (currentRoom != null && currentRoom.contains("taiyin")) {
        defaultFloor = "taiyin_tomb_b1f";
      }
      pos = dungeonManager.getOrCreatePosition(self.getName(), defaultFloor);
    }
    DungeonFloor floor = dungeonManager.getFloor(pos.getFloorId());
    if (floor == null) {
      floor = dungeonManager.getFloor("mozhu_mines_b1f");
      if (floor == null) {
        self.reply("【系統】地牢尚未開啟。");
        return;
      }
      pos = dungeonManager.switchFloor(self.getName(), floor.getId());
    }

    // 處理 leave 撤離指令
    if (input.equals("leave") || input.equals("exit") || input.equals("out")) {
      DungeonTile currentTile = floor.getTile(pos.getX(), pos.getY());
      if (currentTile != null && currentTile.getType() == DungeonTile.TileType.STAIRS_UP) {
        self.setInDungeon(false);
        self.reply("\n\u001B[1;36m🚪【撤出地牢】你沿著向上石階攀爬而出，安全重返地表！\u001B[0m\n");
        if ("mozhu_mines_b1f".equals(floor.getId())) {
          self.setCurrentRoomId("mozhu_mines:mine_entrance");
        } else if ("taiyin_tomb_b1f".equals(floor.getId())) {
          self.setCurrentRoomId("newbie_village:inn");
        } else {
          self.setCurrentRoomId("newbie_village:inn");
        }
        self.getService().getCommandDispatcher().dispatch("look");
        return;
      } else {
        self.reply("⚠️ 此處無返回地表的出口（需走到向上階梯 < 處才能撤出）！");
        return;
      }
    }

    if (battleService.isInBattle(self.getName())) {
      self.reply("\u001B[1;31m⚠️ 戰鬥交鋒中，無法隨意移步！請點擊【⚔️ 迎戰】進攻或【🏃 遁地撤退】！\u001B[0m");
      broadcastDrpgState(self, floor, pos);
      return;
    }

    // 處理無參數、map 或 ascii 指令
    if (input.isEmpty() || input.equals("map")) {
      self.reply("【靈識感應】已掃描" + floor.getName() + " 靈識雷達 [X: " + pos.getX() + ", Y: " + pos.getY() + "]，朝向: " + pos.getFacing().getSymbol() + " " + pos.getFacing().getChineseName() + "。\n"
          + dungeonNavigator.inspectForward(floor, pos));
      broadcastDrpgState(self, floor, pos);
      return;
    }

    if (input.equals("ascii")) {
      self.reply(dungeonNavigator.renderAsciiMap(floor, pos) + "\n" + dungeonNavigator.inspectForward(floor, pos));
      broadcastDrpgState(self, floor, pos);
      return;
    }

    // 處理移動與轉向指令
    switch (input) {
      case "w", "forward", "f", "step w" -> {
        StepResult res = dungeonNavigator.moveForward(floor, pos);
        self.reply(res.message() + (res.success() ? "\n" + dungeonNavigator.inspectForward(floor, pos) : ""));
        handlePostMovement(self, floor, pos, res);
        broadcastDrpgState(self, floor, pos);
      }
      case "s", "back", "b", "step s" -> {
        StepResult res = dungeonNavigator.moveBackward(floor, pos);
        self.reply(res.message() + (res.success() ? "\n" + dungeonNavigator.inspectForward(floor, pos) : ""));
        handlePostMovement(self, floor, pos, res);
        broadcastDrpgState(self, floor, pos);
      }
      case "a", "left", "tl", "turn left" -> {
        StepResult res = dungeonNavigator.turnLeft(pos);
        self.reply(res.message() + "\n" + dungeonNavigator.inspectForward(floor, pos));
        broadcastDrpgState(self, floor, pos);
      }
      case "d", "right", "tr", "turn right" -> {
        StepResult res = dungeonNavigator.turnRight(pos);
        self.reply(res.message() + "\n" + dungeonNavigator.inspectForward(floor, pos));
        broadcastDrpgState(self, floor, pos);
      }
      case "look", "inspect" -> {
        self.reply(dungeonNavigator.inspectForward(floor, pos));
        broadcastDrpgState(self, floor, pos);
      }
      case "dummy", "test", "testmob" -> {
        battleService.startTrainingBattle(self, pos);
      }
      default -> {
        self.reply("【地牢指令】\n"
            + "  dungeon / map              - 展開靈識感應地圖與前方視野\n"
            + "  dungeon ascii              - 印出終端文字版 ASCII 感應地圖\n"
            + "  dungeon enter [地牢代號]   - 踏入指定地牢 (如 mozhu_mines_b1f)\n"
            + "  dungeon leave              - 在向上階梯處撤出地牢回到地表\n"
            + "  dungeon list               - 列出當前開放的所有地牢\n"
            + "  step w / forward           - 向前邁步 (探索/遇敵檢定)\n"
            + "  step s / back              - 向後退步\n"
            + "  step a / left              - 向左轉 90 度\n"
            + "  step d / right             - 向右轉 90 度\n"
            + "  dungeon look               - 凝神探查正前方地塊\n"
            + "  dungeon dummy              - 召喚試道傀儡演練絕學");
      }
    }
  }

  private void handlePostMovement(Player self, DungeonFloor floor, DungeonPosition pos, StepResult res) {
    if (!res.success()) {
      return;
    }

    // 1. 警戒度累積與暗雷檢定
    int delta = ThreadLocalRandom.current().nextInt(8, 17);
    int danger = pos.addDanger(delta);
    if (danger >= 100) {
      triggerRandomEncounter(self, floor, pos);
      pos.resetDanger();
      return;
    }

    // 2. 特殊地塊踩點事件
    handleTileEvent(self, floor, pos);
  }

  private void triggerRandomEncounter(Player self, DungeonFloor floor, DungeonPosition pos) {
    List<String> mobs = (floor.getMobPool() != null && !floor.getMobPool().isEmpty())
        ? floor.getMobPool()
        : TOMB_MOBS;
    int count = ThreadLocalRandom.current().nextInt(1, 3);
    List<String> mobIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      mobIds.add(mobs.get(ThreadLocalRandom.current().nextInt(mobs.size())));
    }
    battleService.startBattle(self, pos, mobIds);
  }

  private void handleTileEvent(Player self, DungeonFloor floor, DungeonPosition pos) {
    DungeonTile tile = floor.getTile(pos.getX(), pos.getY());
    if (tile == null) return;

    switch (tile.getType()) {
      case TREASURE -> {
        if (!pos.isChestOpened(pos.getX(), pos.getY())) {
          pos.markChestOpened(pos.getX(), pos.getY());
          List<String> dropPool = (tile.getDrops() != null && !tile.getDrops().isEmpty())
              ? tile.getDrops()
              : TOMB_ITEMS;
          String itemId = dropPool.get(ThreadLocalRandom.current().nextInt(dropPool.size()));
          var opt = templateReader.findItem(itemId);
          String itemName = opt.map(ItemTemplate::name).orElse("古仙秘寶");
          String itemDesc = opt.map(ItemTemplate::description).orElse("");
          Party party = partyService.getOrCreateParty(self.getName());
          boolean added = party.getInventory().addItem(itemId, 1);
          if (added) {
            self.reply("\n\u001B[1;33m🎁【開啟寶箱】沉重的箱蓋緩緩開啟，靈光流轉！你獲得了寶物：【"
                + itemName + "】！已收納入隊伍行囊！\n" + itemDesc + "\u001B[0m\n");
          } else {
            self.reply("\n\u001B[1;33m🎁【開啟寶箱】沉重的箱蓋緩緩開啟，靈光流轉！發現了寶物【"
                + itemName + "】！\n\u001B[1;31m⚠️ 隊伍行囊已滿，寶物散落於地無法收納！\u001B[0m\n");
          }
          broadcastDrpgState(self, floor, pos);
        } else {
          self.reply("【寶箱】這座寶箱已被開啟，裡面空無一物。");
        }
      }
      case BOSS -> {
        String bossMobId = (tile.getEventId() != null && !tile.getEventId().isBlank())
            ? tile.getEventId()
            : "mozhu_mines:boss_song_tianheng";
        battleService.startBossBattle(self, pos, bossMobId);
      }
      case TRAP -> {
        Party party = partyService.getOrCreateParty(self.getName());
        for (PartyMember m : party.getMembers()) {
          m.takeDamage(15);
          m.consumeSan(5);
        }
        self.reply("\n\u001B[1;35m⚠️【觸發陷阱】地面突然陷落，菌絲煞氣噴湧！全隊受到 15 點腐蝕傷害，道心動搖 (-5 SAN)！\u001B[0m\n");
      }
      case STAIRS_DOWN -> {
        self.reply("\n\u001B[1;36m🏛️【" + tile.getName() + "】" + tile.getDescription() + "\u001B[0m\n");
      }
      case STAIRS_UP -> {
        self.reply("\n\u001B[1;32m🪜【" + tile.getName() + "】" + tile.getDescription() + " (輸入 'dungeon leave' 可返回地表)\u001B[0m\n");
      }
      case DOOR -> {
        self.reply("\n\u001B[1;32m🚪【" + tile.getName() + "】" + tile.getDescription() + "\u001B[0m\n");
      }
      case EVENT -> {
        self.reply("\n\u001B[1;36m📜【" + tile.getName() + "】" + tile.getDescription() + "\u001B[0m\n");
      }
      default -> {}
    }
  }

  public void broadcastDrpgState(Player player, DungeonFloor floor, DungeonPosition pos) {
    if (player == null) {
      return;
    }
    broadcastService.broadcastState(player);
  }

  @Override
  public String getDescription() {
    return "探索 DRPG 網格地牢 (支援 map, step w/s/a/d)";
  }
}
